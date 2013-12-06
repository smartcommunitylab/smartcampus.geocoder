var baseurl = 'http://localhost:8080/core.geocoder/collection1/select';

var mapCenter = [ 11.119885, 46.071832 ];
var defaultZoom = 15;
var defaultInterestRadius = 100;
var spatialField = 'coordinate';

var map = new OpenLayers.Map('map');
var resultsLayer = new OpenLayers.Layer.Markers("results");
var positionLayer = new OpenLayers.Layer.Markers("position");
var bufferLayer = new OpenLayers.Layer.Vector("buffer_distance");

var poiProjection = new OpenLayers.Projection("EPSG:4326"); // WGS 84
var mapProjection = new OpenLayers.Projection("EPSG:900913"); // to Spherical
// Mercator
// Projection

var bufferFid;
var spatial;
var positionMarker;

function init() {
	initMap();

	$('#interest_radius').val(defaultInterestRadius);
	$('#interest_radius').spinner({
		min : 0,
		step : 100,
		numberFormat : 'n',
		stop : function(event, ui) {
			refreshBuffer();
		}
	});

	$('#bmypos').click(
			function() {
				map.events.register('click', map, function(e) {
					spatial = true;
					var lonlat = map.getLonLatFromPixel(e.xy);
					var size = new OpenLayers.Size(32, 32);
					var offset = new OpenLayers.Pixel(-(size.w / 2), -size.h);
					var i = new OpenLayers.Icon('img/demo/positionMarker.png',
							size, offset);
					positionMarker = new OpenLayers.Marker(lonlat,i);
					positionLayer.addMarker(positionMarker);
					var circle = OpenLayers.Geometry.Polygon
							.createRegularPolygon(
									new OpenLayers.Geometry.Point(lonlat.lon,
											lonlat.lat), $('#interest_radius')
											.val(), 50, 0);
					var feature = new OpenLayers.Feature.Vector(circle);
					bufferFid = feature.fid;
					bufferLayer.addFeatures([ feature ]);

					map.zoomToExtent(bufferLayer.getDataExtent());

					map.events.remove('click');
					$('#bmypos').attr('disabled', '');
					$('#mypos_coords').text("Current position is ");
					var zoomToPosition = $('<a>');
					zoomToPosition.text(lonlat.lat + "," + lonlat.lon);
					zoomToPosition.click(function() {
						map.moveTo(lonlat, defaultZoom);
					});

					var removeLink = $('<a>');
					removeLink.text('delete');
					removeLink.click(function() {
						bufferLayer.removeAllFeatures();
						positionLayer.clearMarkers();
						$('#mypos_coords').empty();
						$('#bmypos').removeAttr('disabled');
						spatial = false;
					});
					$('#mypos_coords').append(zoomToPosition).append(" (")
							.append(removeLink).append(")");
				}, false);
			});
	$('#bsearch')
			.click(
					function() {
						
						var input = $('#search-field').val();
						var token = input.split(',');
						var subtoken = token[0].split(' ');
						var querystring = '?q=';
						if (token[0].trim().length == 0) {
							console.log('Empty research');
						} else {
							var queryOnName = 'name:(';
							var queryOnStreet = 'street:(';
							$.each(subtoken, function(k, v) {
								queryOnName += ' +' +v.trim();
								queryOnStreet += ' +' +v.trim();
							});
							queryOnName +=')';
							queryOnStreet +=')';
							querystring += encodeURIComponent('+('+queryOnName+' OR '+queryOnStreet+')');
						}
						if (token.length == 3 && token[1].trim().length > 0) {
							querystring += encodeURIComponent(' +housenumber:'
									+ token[1].trim());
						}

						if (token.length == 2 && token[1]
								&& token[1].trim().length > 0) {
							if($.isNumeric(token[1].trim())){
								querystring += encodeURIComponent(' +housenumber:'
										+ token[1].trim());
							}else{
							querystring += encodeURIComponent(' +city:'
									+ token[1].trim());
							}
						}
						if (token.length == 3 && token[2]
								&& token[2].trim().length > 0) {
							querystring += encodeURIComponent(' +city:'
									+ token[2].trim());
						}

						querystring += '&wt=json&indent=true';
						if (spatial) {
							var transformedCoords = positionMarker.lonlat
									.clone().transform(mapProjection,
											poiProjection);
							querystring += "&fq={!geofilt}&sort=geodist() asc&spatial=true&sfield="
									+ spatialField
									+ "&pt="
									+ transformedCoords.lat
									+ ","
									+ transformedCoords.lon
									+ "&d="
									+ ($('#interest_radius').val() / 1000);
						}
						console.log(querystring);
						$.ajax({
							async : true,
							type : 'GET',
							headers : {},
							url : baseurl + querystring,

							success : function(data, textStatus, errorThrown) {
								//clean all popups
								$.each(map.popups, function(k, v) {
									map.removePopup(v);
								});
								
								// clean map
								resultsLayer.clearMarkers();
								// display marker in map
								$.each(data['response']['docs'],
										function(k, v) {
											popolateMap(v);
										});
								$('#display').text(
										JSON.stringify(data, null, 2));

								$('#results').text("Element founded: ");
								var numFound = $('<span>');
								numFound.addClass('badge');
								numFound.text(data['response']['numFound']);
								$('#results').append(numFound);
								// set map extent to include all the results
								if (data['response']['numFound'] > 0) {
									map.zoomToExtent(resultsLayer
											.getDataExtent());
								}
							},
							error : function(jqXHR, textStatus, errorThrown) {
								alert('Error during operation: ' + textStatus
										+ ' ' + errorThrown);
							}
						});
					});

	$('#search-field').keypress(function(evt) {
		if (evt.which == 13) {
			$('#bsearch').click();
		}
	});

}

function popolateMap(poi) {
	// console.log('marker pos: '+poi['coordinate']);
	var coords = poi['coordinate'].split(',');
	var position = new OpenLayers.LonLat(coords[1], coords[0]).transform(
			poiProjection, mapProjection);

	var marker = new OpenLayers.Marker(position, OpenLayers.Marker
			.defaultIcon());
	marker.events.register("click", marker, function(e) {
		// remove all popup on map
		$.each(map.popups, function(k, v) {
			map.removePopup(v);
		});
		var popup = new OpenLayers.Popup.FramedCloud('' + poi['id'], position,
				new OpenLayers.Size(250, 250), markerPopup(poi), marker.icon,
				true);

		map.addPopup(popup);
	});

	resultsLayer.addMarker(marker);
}

function refreshBuffer() {
	var buffer = bufferLayer.getFeatureByFid(bufferFid);
	if (buffer && $.isNumeric($('#interest_radius').val())) {
		bufferLayer.removeAllFeatures();
		var circle = OpenLayers.Geometry.Polygon.createRegularPolygon(
				buffer.geometry.getCentroid(), $('#interest_radius').val(), 50,
				0);
		var feature = new OpenLayers.Feature.Vector(circle);
		bufferFid = feature.fid;
		bufferLayer.addFeatures([ feature ]);

		map.zoomToExtent(bufferLayer.getDataExtent());
	}
}

function markerPopup(marker) {
	var details = '';
	if (marker['name'])
		details += '<b>name</b>: ' + marker['name'] + '<br/>';
	if (marker['osm_key'])
		details += '<b>osm_key</b>: ' + marker['osm_key'] + '<br/>';
	if (marker['osm_value'])
		details += '<b>osm_value</b>: ' + marker['osm_value'] + '<br/>';
	if (marker['street'])
		details += '<b>street</b>: ' + marker['street'] + '<br/>';
	if (marker['postcode'])
		details += '<b>postcode</b>: ' + marker['postcode'] + '<br/>';
	if (marker['city'])
		details += '<b>city</b>: ' + marker['city'] + '<br/>';
	if (marker['country'])
		details += '<b>country</b>: ' + marker['country'] + '<br/>';
	if (marker['places'])
		details += '<b>places</b>: ' + marker['places'];
	return details;
}
function initMap() {
	map = new OpenLayers.Map('map', {
		unit : 'm'
	});
	var mapnik = new OpenLayers.Layer.OSM('osm-background');
	var position = new OpenLayers.LonLat(mapCenter).transform(poiProjection,
			mapProjection);
	map.addLayers([ mapnik, positionLayer, bufferLayer, resultsLayer ]);
	map.setCenter(position, defaultZoom);
}
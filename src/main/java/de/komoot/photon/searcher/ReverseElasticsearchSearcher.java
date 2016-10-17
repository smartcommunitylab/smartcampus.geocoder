package de.komoot.photon.searcher;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

/**
 *
 * @author svantulden
 */
public class ReverseElasticsearchSearcher implements ElasticsearchReverseSearcher {
    private Client client;

    public ReverseElasticsearchSearcher(Client client) {
        this.client = client;
    }
    
    @Override
    public SearchResponse search(QueryBuilder queryBuilder, Integer limit, Point location) {
        TimeValue timeout = TimeValue.timeValueSeconds(7);        
        return client.prepareSearch("photon").
                setSearchType(SearchType.QUERY_AND_FETCH)
                            .setQuery(queryBuilder)
                            .addSort(SortBuilders.geoDistanceSort("coordinate").point(location.getY(), location.getX()).order(SortOrder.ASC))
                            .setSize(limit)
                            .setTimeout(timeout)
                            .execute()
                            .actionGet();

    }
}

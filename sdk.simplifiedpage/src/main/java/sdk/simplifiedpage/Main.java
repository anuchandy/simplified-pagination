package sdk.simplifiedpage;

import java.util.List;

public class Main {
    public static void main( String[] args ) {
        APIClient apiClient = new APIClient();
        //
        APIClient.PagedFlowable<Integer> itemsFlowable = apiClient.listAsync();
        //
        // Emit all items in the Flowable.
        itemsFlowable.map(integer -> {
            System.out.println(integer);
            return integer.toString();
        }).blockingSubscribe();
        //
        // Emit only 10 items in the Flowable.
        itemsFlowable.take(10)
                .map(integer -> {
                    System.out.println(integer);
                    return integer.toString();
                }).blockingSubscribe();
        //
        // Convert Flowable to list.
        //
        List<Integer> list = itemsFlowable.asList();
        for (Integer item : list) {
            System.out.println(item);
        }
    }
}

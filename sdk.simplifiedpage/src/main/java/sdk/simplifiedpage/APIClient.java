package sdk.simplifiedpage;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.rest.RestException;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.reactivestreams.Subscriber;
import rx.exceptions.Exceptions;
import java.util.ArrayList;
import java.util.List;

public class APIClient {
    public PagedFlowable<Integer> listAsync() {
        Flowable<Integer> backingFlowable = getFirstPageAsync()
                .toFlowable()
                .concatMap((Page<Integer> page) -> {
                    final String nextNextLink = page.nextPageLink();
                    if (nextNextLink == null) {
                        return Flowable.just(page);
                    }
                    return Flowable.just(page).concatWith(ListFromSecondPageAsync(nextNextLink));
            }).flatMapIterable((Page<Integer> page) -> page.items());
        //
        Function<String, Single<Page<Integer>>> getNextPageAsyncFunc = this::getNextPageAsync;
        return new PagedFlowable(backingFlowable, getFirstPageAsync(), getNextPageAsyncFunc);
    }

    private Single<Page<Integer>> getFirstPageAsync() {
        return Single.fromCallable(() -> generateDataPage("0"));
    }

    private Single<Page<Integer>> getNextPageAsync(final String nextLink) {
        if (nextLink == null) {
            return Single.error(new RuntimeException("nextLink is empty"));
        } else {
            return Single.fromCallable(() -> generateDataPage(nextLink));
        }
    }

    private Flowable<Page<Integer>> ListFromSecondPageAsync(final String nextLink) {
        if (nextLink == null) {
            return Flowable.empty();
        } else {
            return getNextPageAsync(nextLink).toFlowable().concatMap((Page<Integer> page) -> {
                final String nextNextLink = page.nextPageLink();
                if (nextNextLink == null) {
                    return Flowable.just(page);
                }
                return Flowable.just(page).concatWith(ListFromSecondPageAsync(nextNextLink));
            });
        }
    }

    private static Page<Integer> generateDataPage(String nextLink) {
        final List<Integer> items = new ArrayList<Integer>();
        final Integer from = Integer.parseInt(nextLink);
        final Integer to = from + 10;
        //
        for (int i = from; i < to; i++) {
            items.add(i);
        }
        //
        return new Page<Integer>() {
            @Override
            public String nextPageLink() {
                if (to > 100) {
                    return null;
                } else {
                    return String.valueOf(to);
                }
            }

            @Override
            public List<Integer> items() {
                return items;
            }
        };
    }

    //
    public class PagedFlowable<T> extends Flowable<T> {
        private final Flowable<T> backingFlowable;
        private final Single<Page<T>> firstPageSingle;
        private final Function<String, Single<Page<T>>> getNextPageAsyncFunc;

        PagedFlowable(Flowable<T> backingFlowable,
                      Single<Page<T>> firstPageSingle,
                      Function<String, Single<Page<T>>> getNextPageAsyncFunc) {
            this.backingFlowable = backingFlowable;
            this.firstPageSingle = firstPageSingle;
            this.getNextPageAsyncFunc = getNextPageAsyncFunc;
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> s) {
            backingFlowable.subscribe(s);
        }

        public PagedList<T> asList() {
            return new PagedList<T>(this.firstPageSingle.blockingGet()) {
                @Override
                public Page<T> nextPage(String nextPageLink) throws RestException {
                    try {
                        return getNextPageAsyncFunc.apply(nextPageLink).blockingGet();
                    } catch (Exception ex) {
                        throw Exceptions.propagate(ex);
                    }
                }
            };
        }
    }
}

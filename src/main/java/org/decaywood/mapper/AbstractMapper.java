package org.decaywood.mapper;

import org.decaywood.AbstractService;
import org.decaywood.Mapper;
import org.decaywood.entity.DeepCopy;
import org.decaywood.timeWaitingStrategy.TimeWaitingStrategy;
import org.decaywood.utils.HttpRequestHelper;
import org.decaywood.utils.URLMapper;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author: decaywood
 * @date: 2015/11/24 16:56
 */

public abstract class AbstractMapper <T, R> extends AbstractService implements Mapper<T, R> {


    protected abstract R mapLogic(T t) throws Exception;


    public AbstractMapper(TimeWaitingStrategy strategy) {
        this(strategy, URLMapper.MAIN_PAGE.toString());
    }

    public AbstractMapper(TimeWaitingStrategy strategy, String webSite) {
        super(strategy, webSite);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R apply(T t) {

        if (t != null)
            System.out.println(getClass().getSimpleName() + " mapping -> " + t.getClass().getSimpleName());

        R res = null;
        int retryTime = this.strategy.retryTimes();

        try {

            int loopTime = 1;
            boolean needRMI = true;

            if(t != null) t = t instanceof DeepCopy ? ((DeepCopy<T>) t).copy() : t;

            while (retryTime > loopTime) {
                try {
                    res = mapLogic(t);
                    needRMI = false;
                    break;
                } catch (Exception e) {
                    if (!(e instanceof IOException)) throw e;
                    System.out.println("Mapper: Network busy Retrying -> " + loopTime + " times");
                    HttpRequestHelper.updateCookie(webSite);
                    this.strategy.waiting(loopTime++);
                }
            }

            if (needRMI && rmiClient) {
                AbstractMapper proxy = (AbstractMapper) getRMIProxy();
                //noinspection unchecked
                res = (R) proxy.apply(t);
            } else throw new TimeoutException("Request Time Out, You've been Possibly Banned");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;

    }
}

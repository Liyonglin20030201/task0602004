package com.trace.flink.metrics.function;

import com.trace.common.model.MetricPoint;
import com.trace.flink.metrics.model.MetricsAccumulator;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class MetricsWindowFunction
        extends ProcessWindowFunction<MetricsAccumulator, MetricPoint, String, TimeWindow> {

    @Override
    public void process(String key,
                        ProcessWindowFunction<MetricsAccumulator, MetricPoint, String, TimeWindow>.Context context,
                        Iterable<MetricsAccumulator> elements,
                        Collector<MetricPoint> out) {
        MetricsAccumulator acc = elements.iterator().next();
        TimeWindow window = context.window();

        MetricPoint point = new MetricPoint();
        point.setServiceName(acc.getServiceName());
        point.setEndpoint(acc.getOperationName());
        point.setWindowStart(window.getStart());
        point.setWindowEnd(window.getEnd());
        point.setTotalCount(acc.getTotalCount());
        point.setSuccessRate(acc.getSuccessRate());
        point.setP50Latency(acc.getP50());
        point.setP99Latency(acc.getP99());

        out.collect(point);
    }
}

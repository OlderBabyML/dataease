package io.dataease.service.userQuery;

import com.alibaba.fastjson.JSONObject;
import io.dataease.commons.utils.DateUtils;
import io.dataease.dto.userQuery.UserInfoDTO;
import io.dataease.plugins.common.base.domain.DatasetTableField;
import io.dataease.plugins.common.base.domain.Datasource;
import io.dataease.plugins.common.constants.DatasourceTypes;
import io.dataease.plugins.common.constants.DeTypeConstants;
import io.dataease.plugins.common.dto.chart.ChartCustomFilterItemDTO;
import io.dataease.plugins.common.dto.chart.ChartFieldCustomFilterDTO;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import io.dataease.provider.ProviderFactory;
import io.dataease.provider.datasource.JdbcProvider;
import io.dataease.provider.query.impala.ImpalaQueryProvider;
import io.dataease.service.datasource.DatasourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class UserQueryService {

    @Resource
    private DatasourceService datasourceService;

    private static String micoDw = "mico_dw";

    private static String yohoDw = "yoho_dw";

    private static String somatchDw = "somatch_dw";

    private static String micoAl = "mico_al";

    private static String yohoAl = "yoho_al";

    private static String somatchAl = "somatch_al";

    private static String topAl = "top_al";

    private static String topDw = "top_dw";

    private static String toptopDw = "toptop_dw";

    public List<JSONObject> getUserInfo(UserInfoDTO userInfoDTO) throws Exception {
        List<JSONObject> users = new ArrayList<>();
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        ImpalaQueryProvider qp = (ImpalaQueryProvider) ProviderFactory.getQueryProvider(DatasourceTypes.impala.getType());
        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
        List<ChartFieldCustomFilterDTO> requestList = new ArrayList<>();
        if (userInfoDTO.getUserId() != null) {
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = getChartFieldCustomFilterDTO("eq", "logic", userInfoDTO.getUserId().toString());
            chartFieldCustomFilterDTO.setField(DatasetTableField.builder().originName("user_id").deType(DeTypeConstants.DE_INT).deExtractType(DeTypeConstants.DE_INT).build());
            requestList.add(chartFieldCustomFilterDTO);
        }
        if (userInfoDTO.getUid() != null) {
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = getChartFieldCustomFilterDTO("eq", "logic", userInfoDTO.getUid().toString());
            chartFieldCustomFilterDTO.setField(DatasetTableField.builder().originName("uid").deType(DeTypeConstants.DE_INT).deExtractType(DeTypeConstants.DE_INT).build());
            requestList.add(chartFieldCustomFilterDTO);
        }
        if (userInfoDTO.getCountry() != null) {
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = getChartFieldCustomFilterDTO("eq", "logic", userInfoDTO.getCountry());
            chartFieldCustomFilterDTO.setField(DatasetTableField.builder().originName("country").deType(DeTypeConstants.DE_STRING).deExtractType(DeTypeConstants.DE_STRING).build());
            requestList.add(chartFieldCustomFilterDTO);
        }
        ChartFieldCustomFilterDTO startTime = getChartFieldCustomFilterDTO("eq", "logic", DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2, "yyyyMMdd"));
        startTime.setField(DatasetTableField.builder().originName("dt").deType(DeTypeConstants.DE_STRING).deExtractType(DeTypeConstants.DE_STRING).build());
        requestList.add(startTime);
        String product = micoDw;
        if (userInfoDTO.getProduct().equals("Yoho")) {
            product = yohoDw;
        }
        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
        if (CollectionUtils.isNotEmpty(dw)) {
            startTime.getFilter().get(0).setValue(DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24, "yyyyMMdd"));
            String querySql = qp.getSQLSummaryNoView(product + ".ads_user_attr", requestList, null, "dt", "desc");
            datasourceRequest.setDatasource(dw.get(0));
            datasourceRequest.setTable(product + ".ads_user_attr");
            datasourceRequest.setQuery(querySql + " limit 1");
            List<JSONObject> jsonData = jdbcProvider.getJsonData(datasourceRequest);
            if (CollectionUtils.isNotEmpty(jsonData)) {
                users = jsonData;
            } else {
                startTime.getFilter().get(0).setValue(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"));
                querySql = qp.getSQLSummaryNoView(product + ".ads_new_user_attr_hr", requestList, null, "dt", "desc");
                datasourceRequest.setDatasource(dw.get(0));
                datasourceRequest.setTable(product + ".ads_new_user_attr_hr");
                datasourceRequest.setQuery(querySql + " limit 1");
                jsonData = jdbcProvider.getJsonData(datasourceRequest);
                if (CollectionUtils.isNotEmpty(jsonData)) {
                    users = jsonData;
                } else {
                    startTime.getFilter().get(0).setValue(DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2, "yyyyMMdd"));
                    querySql = qp.getSQLSummaryNoView(product + ".ads_user_attr", requestList, null, "dt", "desc");
                    datasourceRequest.setDatasource(dw.get(0));
                    datasourceRequest.setTable(product + ".ads_user_attr");
                    datasourceRequest.setQuery(querySql + " limit 1");
                    jsonData = jdbcProvider.getJsonData(datasourceRequest);
                    if (CollectionUtils.isNotEmpty(jsonData)) {
                        users = jsonData;
                    }
                }
            }
        }
        return users;
    }

    public List<JSONObject> getOptions(UserInfoDTO userInfoDTO) throws Exception {
        ArrayList<JSONObject> options = new ArrayList<>();
        JSONObject server = new JSONObject();
        server.put("id", "server");
        server.put("label", "服务端埋点");
        server.put("children", new ArrayList<JSONObject>());
        JSONObject client = new JSONObject();
        client.put("id", "client");
        client.put("label", "客户端埋点");
        client.put("children", new ArrayList<JSONObject>());
        JSONObject h5 = new JSONObject();
        h5.put("id", "h5");
        h5.put("label", "h5埋点");
        h5.put("children", new ArrayList<JSONObject>());
        JSONObject technology = new JSONObject();
        technology.put("id", "technology");
        technology.put("label", "技术埋点");
        technology.put("children", new ArrayList<JSONObject>());
        options.add(server);
        options.add(client);
        options.add(h5);
        options.add(technology);
        String product = micoAl;
        if (userInfoDTO.getProduct().equals("Yoho")) {
            product = yohoAl;
        } else if (userInfoDTO.getProduct().equals("SoMatch")) {
            product = somatchAl;
        } else if (userInfoDTO.getProduct().equals("TopTop")) {
            product = topAl;
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
        if (CollectionUtils.isNotEmpty(dw)) {
            String sql = "select event_type,event_name from " + product + ".al_north_star_event_type_mapping where dt='" + DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24, "yyyyMMdd") + "'";
            datasourceRequest.setDatasource(dw.get(0));
            datasourceRequest.setQuery(sql);
            List<JSONObject> jsonData = jdbcProvider.getJsonRowData(datasourceRequest);
            if (jsonData.size() == 0) {
                datasourceRequest.setQuery(sql);
                jsonData = jdbcProvider.getJsonRowData(datasourceRequest);
            }
            for (JSONObject object : jsonData) {
                String type = object.getString("event_type");
                switch (type) {
                    case "server":
                        ArrayList children = server.getObject("children", ArrayList.class);
                        JSONObject o = new JSONObject();
                        o.put("type", type);
                        o.put("id", object.getString("event_name"));
                        o.put("label", object.getString("event_name"));
                        children.add(o);
                        server.put("children", children);
                        break;
                    case "client":
                        ArrayList children1 = client.getObject("children", ArrayList.class);
                        String eventName = object.getString("event_name");
                        if (!StringUtils.isEmpty(eventName)) {
                            JSONObject o1 = new JSONObject();
                            o1.put("type", type);
                            o1.put("id", object.getString("event_name"));
                            o1.put("label", object.getString("event_name"));
                            children1.add(o1);
                            client.put("children", children1);
                        }
                        break;
                    case "h5":
                        ArrayList children2 = h5.getObject("children", ArrayList.class);
                        JSONObject o2 = new JSONObject();
                        o2.put("type", type);
                        o2.put("id", object.getString("event_name"));
                        o2.put("label", object.getString("event_name"));
                        children2.add(o2);
                        h5.put("children", children2);
                        break;
                    case "technology":
                        ArrayList children3 = technology.getObject("children", ArrayList.class);
                        JSONObject o3 = new JSONObject();
                        o3.put("type", type);
                        o3.put("id", object.getString("event_name"));
                        o3.put("label", object.getString("event_name"));
                        children3.add(o3);
                        technology.put("children", children3);
                        break;
                }
            }
        }
        List<JSONObject> objects = options.stream().filter(new Predicate<JSONObject>() {
            @Override
            public boolean test(JSONObject jsonObject) {
                List children = jsonObject.getObject("children", List.class);
                return children.size() > 0;
            }
        }).collect(Collectors.toList());
        return objects;
    }

    public JSONObject getUserEventNum(UserInfoDTO userInfoDTO) throws Exception {
        JSONObject event = new JSONObject();
        String product = micoDw;
        String columnName = "user_id";
        if (userInfoDTO.getProduct().equals("Yoho")) {
            product = yohoDw;
            columnName = "uid";
        } else if (userInfoDTO.getProduct().equals("SoMatch")) {
            product = somatchDw;
            columnName = "uid";
        } else if (userInfoDTO.getProduct().equals("TopTop")) {
            product = topDw;
            columnName = "uid";
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
//        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
//        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
        List<JSONObject> eventList = userInfoDTO.getEventList();
        HashMap<String, List<String>> nodeHashMap = new HashMap<>();
        for (JSONObject o : eventList) {
            List<String> events = nodeHashMap.get(o.getString("type"));
            if (events != null) {
                events.add("'" + o.getString("id") + "'");
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add("'" + o.getString("id") + "'");
                nodeHashMap.put(o.getString("type"), list);
            }
        }
        Set<Map.Entry<String, List<String>>> entries = nodeHashMap.entrySet();
        ArrayList<String> arrayList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case "server":
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".dwd_server_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    if (DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd").equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select dt,event_name,count(*) ct from " + product + ".tl_server_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    }
                    break;
                case "client":
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".dwd_client_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    if (DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd").equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select dt,event_name,count(*) ct from " + product + ".tl_client_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    }
                    break;
                case "h5":
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".dwd_h5_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    if (DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd").equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select dt,event_name,count(*) ct from " + product + ".tl_h5_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    }
                    break;
                case "technology":
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".dwd_apm_client_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    break;
            }
        }
        String sql = String.join(" union all ", arrayList);
        sql = "select dt,event_name,sum(ct) ct from (" + sql + ") t group by dt,event_name order by dt";
//        datasourceRequest.setDatasource(dw.get(0));
//        datasourceRequest.setQuery(sql);
        List<JSONObject> jsonData = queryPresto(sql);
        ArrayList<JSONObject> charts = new ArrayList<>();
        ArrayList<JSONObject> pips = new ArrayList<>();
        for (JSONObject object : jsonData) {
            boolean chartFlag = true;
            boolean pipFlag = true;
            String dt = object.getString("dt");
            String eventName = object.getString("event_name");
            Long ct = object.getLong("ct");
            for (JSONObject chart : charts) {
                if (chart.getString("dt").equals(dt)) {
                    chartFlag = false;
                    chart.put("ct", chart.getLong("ct") + ct);
                }
            }
            if (chartFlag) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("dt", dt);
                jsonObject.put("ct", ct);
                charts.add(jsonObject);
            }
            for (JSONObject pip : pips) {
                if (pip.getString("name").equals(eventName)) {
                    pipFlag = false;
                    pip.put("value", pip.getLong("value") + ct);
                }
            }
            if (pipFlag) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", eventName);
                jsonObject.put("value", ct);
                pips.add(jsonObject);
            }
        }
        ArrayList<String> x = new ArrayList<>();
        ArrayList<Long> y = new ArrayList<>();
        for (JSONObject chart : charts) {
            x.add(chart.getString("dt"));
            y.add(chart.getLong("ct"));
        }
        event.put("x", x);
        event.put("y", y);
        event.put("pip", pips);
        return event;
    }

    public ArrayList<JSONObject> getUserEventDetail(UserInfoDTO userInfoDTO) throws Exception {
        ArrayList<JSONObject> eventDetailList = new ArrayList<>();
        String product = micoDw;
        String columnName = "user_id";
        if (userInfoDTO.getProduct().equals("Yoho")) {
            columnName = "uid";
            product = yohoDw;
        } else if (userInfoDTO.getProduct().equals("SoMatch")) {
            columnName = "uid";
            product = somatchDw;
        } else if (userInfoDTO.getProduct().equals("TopTop")) {
            columnName = "uid";
            product = topDw;
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
//        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
//        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
        List<JSONObject> eventList = userInfoDTO.getEventList();
        HashMap<String, List<String>> nodeHashMap = new HashMap<>();
        for (JSONObject o : eventList) {
            List<String> events = nodeHashMap.get(o.getString("type"));
            if (events != null) {
                events.add("'" + o.getString("id") + "'");
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add("'" + o.getString("id") + "'");
                nodeHashMap.put(o.getString("type"), list);
            }
        }
        Set<Map.Entry<String, List<String>>> entries = nodeHashMap.entrySet();
        ArrayList<String> arrayList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case "server":
                    arrayList.add("select * from " + product + ".dwd_server_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    if (DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd").equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select * from " + product + ".tl_server_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    }
                    break;
                case "client":
                    arrayList.add("select * from " + product + ".dwd_client_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    if (DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd").equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select * from " + product + ".tl_client_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    }
                    break;
                case "h5":
                    arrayList.add("select * from " + product + ".dwd_h5_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    if (DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd").equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select * from " + product + ".tl_h5_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    }
                    break;
                case "technology":
                    arrayList.add("select * from " + product + ".dwd_apm_client_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    break;
            }
        }
        //String sql = String.join(" union all ", arrayList);
        //sql = "select * from (" + sql + ") t order by `timestamp` " + userInfoDTO.getSort();
        if (arrayList.size() > 0) {
            for (String s : arrayList) {
                String sql = "select * from (" + s + ") t ";
//                datasourceRequest.setDatasource(dw.get(0));
//                datasourceRequest.setQuery(sql);
                List<JSONObject> jsonData = queryPresto(sql);
                for (JSONObject object : jsonData) {
                    String dt = DateUtils.getDateString(DateUtils.getDate(object.getString("dt"), "yyyyMMdd"));
                    Long timestamp = object.getLong("timestamp");
                    if (timestamp == null) {
                        timestamp = DateUtils.getDate(object.getString("event_time"), DateUtils.TIME_PATTERN).getTime();
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("isShow", false);
                    jsonObject.put("name", object.getString("event_name"));
                    jsonObject.put("timestamp", DateUtils.getDateStringTimeZone(timestamp, "HH:mm:ss:SSS"));
                    jsonObject.put("timestamps", timestamp);
                    ArrayList<JSONObject> dataList = new ArrayList<>();
                    Set<Map.Entry<String, Object>> set = object.entrySet();
                    for (Map.Entry<String, Object> objectEntry : set) {
                        JSONObject event = new JSONObject();
                        event.put("name", objectEntry.getKey());
                        event.put("value", objectEntry.getValue());
                        dataList.add(event);
                    }
                    jsonObject.put("dataList", dataList);

                    List jsonObjects = null;
                    if (eventDetailList.size() > 0) {
                        for (JSONObject o : eventDetailList) {
                            if (o.getString("date").equals(dt)) {
                                jsonObjects = o.getObject("eventList", List.class);
                            }
                        }
                    }
                    if (jsonObjects != null) {
                        jsonObjects.add(jsonObject);
                    } else {
                        jsonObjects = new ArrayList<>();
                        jsonObjects.add(jsonObject);
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("date", dt);
                        jsonObject1.put("date1", object.getString("dt"));
                        jsonObject1.put("eventList", jsonObjects);
                        eventDetailList.add(jsonObject1);
                    }
                }
            }
            for (JSONObject jsonObject : eventDetailList) {
                List events = jsonObject.getObject("eventList", List.class);
                Collections.sort(events, new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        JSONObject jsonObject1 = JSONObject.parseObject(o1.toString());
                        JSONObject jsonObject2 = JSONObject.parseObject(o2.toString());
                        Long timestamp1 = jsonObject1.getLong("timestamps");
                        Long timestamp2 = jsonObject2.getLong("timestamps");
                        if (userInfoDTO.getSort().equals("desc")) {
                            Long cha = timestamp2 - timestamp1;
                            return cha.intValue();
                        } else {
                            Long cha = timestamp1 - timestamp2;
                            return cha.intValue();
                        }
                    }
                });

                jsonObject.put("eventList", events);
            }
            Collections.sort(eventDetailList, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    Integer date = o1.getInteger("date1");
                    Integer date1 = o2.getInteger("date1");
                    if (userInfoDTO.getSort().equals("desc")) {
                        return date1 - date;
                    } else {
                        return date - date1;
                    }
                }
            });
        }
        ArrayList<JSONObject> results = new ArrayList<>();
        if (eventDetailList.size() >= userInfoDTO.getPageNo() * 50) {
            for (int i = 0; i <= userInfoDTO.getPageNo() * 50 - 1; i++) {
                results.add(eventDetailList.get(i));
            }
            return results;
        }
        return eventDetailList;
    }

    public ArrayList<JSONObject> getUserEventDetailForDate(UserInfoDTO userInfoDTO) throws Exception {
        ArrayList<JSONObject> eventDetailList = new ArrayList<>();
        String product = micoDw;
        String columnName = "user_id";
        if (userInfoDTO.getProduct().equals("Yoho")) {
            columnName = "uid";
            product = yohoDw;
        } else if (userInfoDTO.getProduct().equals("SoMatch")) {
            columnName = "uid";
            product = somatchDw;
        } else if (userInfoDTO.getProduct().equals("TopTop")) {
            columnName = "uid";
            product = topDw;
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
//        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
//        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
        List<JSONObject> eventList = userInfoDTO.getEventList();
        HashMap<String, List<String>> nodeHashMap = new HashMap<>();
        for (JSONObject o : eventList) {
            List<String> events = nodeHashMap.get(o.getString("type"));
            if (events != null) {
                events.add("'" + o.getString("id") + "'");
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add("'" + o.getString("id") + "'");
                nodeHashMap.put(o.getString("type"), list);
            }
        }
        Set<Map.Entry<String, List<String>>> entries = nodeHashMap.entrySet();
        ArrayList<String> arrayList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case "server":
                    if (!userInfoDTO.getDate().equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select * from " + product + ".dwd_server_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt>='" + userInfoDTO.getDate() + "' and dt<='" +
                                userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    } else {
                        arrayList.add("select * from " + product + ".tl_server_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    }
                    break;
                case "client":
                    if (!userInfoDTO.getDate().equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select * from " + product + ".dwd_client_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt>='" + userInfoDTO.getDate() + "' and dt<='" +
                                userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    } else {
                        arrayList.add("select * from " + product + ".tl_client_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    }
                    break;
                case "h5":
                    if (!userInfoDTO.getDate().equals(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"))) {
                        arrayList.add("select * ct from " + product + ".dwd_h5_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt>='" + userInfoDTO.getDate() + "' and dt<='" +
                                userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    } else {
                        arrayList.add("select * from " + product + ".tl_h5_event_hr where " + columnName + "=" + userInfoDTO.getUserId()
                                + " and dt='" + userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    }
                    break;
                case "technology":
                    arrayList.add("select * from " + product + ".dwd_apm_client_event_detail where " + columnName + "=" + userInfoDTO.getUserId()
                            + " and dt>='" + userInfoDTO.getDate() + "' and dt<='" +
                            userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") limit " + userInfoDTO.getPageNo() * 50);
                    break;
            }
        }
//        String sql = String.join(" union all ", arrayList);
//        sql = "select * from (" + sql + ") t order by `timestamp` " + userInfoDTO.getSort();
        if (arrayList.size() > 0) {
            for (String s : arrayList) {
                String sql = "select * from (" + s + ") t ";
//                datasourceRequest.setDatasource(dw.get(0));
//                datasourceRequest.setQuery(sql);
                List<JSONObject> jsonData = queryPresto(sql);
                for (JSONObject object : jsonData) {
                    String dt = DateUtils.getDateString(DateUtils.getDate(object.getString("dt"), "yyyyMMdd"));
                    Long timestamp = object.getLong("timestamp");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("isShow", false);
                    jsonObject.put("name", object.getString("event_name"));
                    jsonObject.put("timestamp", DateUtils.getDateStringTimeZone(timestamp, "HH:mm:ss:SSS"));
                    jsonObject.put("timestamps", timestamp);
                    ArrayList<JSONObject> dataList = new ArrayList<>();
                    Set<Map.Entry<String, Object>> set = object.entrySet();
                    for (Map.Entry<String, Object> objectEntry : set) {
                        JSONObject event = new JSONObject();
                        event.put("name", objectEntry.getKey());
                        event.put("value", objectEntry.getValue());
                        dataList.add(event);
                    }
                    jsonObject.put("dataList", dataList);

                    List jsonObjects = null;
                    if (eventDetailList.size() > 0) {
                        for (JSONObject o : eventDetailList) {
                            if (o.getString("date").equals(dt)) {
                                jsonObjects = o.getObject("eventList", List.class);
                            }
                        }
                    }
                    if (jsonObjects != null) {
                        jsonObjects.add(jsonObject);
                    } else {
                        jsonObjects = new ArrayList<>();
                        jsonObjects.add(jsonObject);
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("date", dt);
                        jsonObject1.put("date1", object.getString("dt"));
                        jsonObject1.put("eventList", jsonObjects);
                        eventDetailList.add(jsonObject1);
                    }
                }
            }
            for (JSONObject jsonObject : eventDetailList) {
                List events = jsonObject.getObject("eventList", List.class);
                Collections.sort(events, new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        JSONObject jsonObject1 = JSONObject.parseObject(o1.toString());
                        JSONObject jsonObject2 = JSONObject.parseObject(o2.toString());
                        Long timestamp1 = jsonObject1.getLong("timestamps");
                        Long timestamp2 = jsonObject2.getLong("timestamps");
                        if (userInfoDTO.getSort().equals("desc")) {
                            Long cha = timestamp2 - timestamp1;
                            return cha.intValue();
                        } else {
                            Long cha = timestamp1 - timestamp2;
                            return cha.intValue();
                        }
                    }
                });

                jsonObject.put("eventList", events);
            }
            Collections.sort(eventDetailList, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    Integer date = o1.getInteger("date1");
                    Integer date1 = o2.getInteger("date1");
                    if (userInfoDTO.getSort().equals("desc")) {
                        return date1 - date;
                    } else {
                        return date - date1;
                    }
                }
            });
        }
        ArrayList<JSONObject> results = new ArrayList<>();
        if (eventDetailList.size() >= userInfoDTO.getPageNo() * 50) {
            for (int i = 0; i <= userInfoDTO.getPageNo() * 50 - 1; i++) {
                results.add(eventDetailList.get(i));
            }
            return results;
        }
        return eventDetailList;
    }

    public List<JSONObject> getUserList(UserInfoDTO userInfoDTO) throws Exception {
        List<JSONObject> users = new ArrayList<>();
        String tableName = "al_north_star_user_attr";
        String productHr = micoDw;
        String product = micoAl;
        if (userInfoDTO.getProduct().equals("Yoho")) {
            product = yohoAl;
            productHr = yohoDw;
        } else if (userInfoDTO.getProduct().equals("SoMatch")) {
            product = somatchDw;
            productHr = somatchDw;
            tableName = "ads_user_attr";
        } else if (userInfoDTO.getProduct().equals("TopTop")) {
            product = topDw;
            productHr = topDw;
            tableName = "ads_user_attr";
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        ImpalaQueryProvider qp = (ImpalaQueryProvider) ProviderFactory.getQueryProvider(DatasourceTypes.impala.getType());
        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
        List<ChartFieldCustomFilterDTO> requestList = new ArrayList<>();
        ChartFieldCustomFilterDTO startTime = getChartFieldCustomFilterDTO("eq", "logic", DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2, "yyyyMMdd"));
        startTime.setField(DatasetTableField.builder().originName("dt").deType(DeTypeConstants.DE_STRING).deExtractType(DeTypeConstants.DE_STRING).build());
        requestList.add(startTime);
        if (userInfoDTO.getUserId() != null) {
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = getChartFieldCustomFilterDTO("eq", "logic", userInfoDTO.getUserId().toString());
            chartFieldCustomFilterDTO.setField(DatasetTableField.builder().originName("user_id").deType(DeTypeConstants.DE_INT).deExtractType(DeTypeConstants.DE_INT).build());
            requestList.add(chartFieldCustomFilterDTO);
            List<JSONObject> userList = getUserList(product, productHr, tableName, startTime, qp, "user_id", datasourceRequest, requestList, userInfoDTO.getUserId(), jdbcProvider);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", userInfoDTO.getUserId().toString());
            jsonObject.put("userId", userInfoDTO.getUserId().toString());
            jsonObject.put("userInfo", userList);
            users.add(jsonObject);
            return users;
        }
        if (userInfoDTO.getUid() != null) {
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = getChartFieldCustomFilterDTO("eq", "logic", userInfoDTO.getUid().toString());
            chartFieldCustomFilterDTO.setField(DatasetTableField.builder().originName("uid").deType(DeTypeConstants.DE_INT).deExtractType(DeTypeConstants.DE_INT).build());
            requestList.add(chartFieldCustomFilterDTO);
            List<JSONObject> userList = null;
            userList = getUserList(product, productHr, tableName, startTime, qp, "uid", datasourceRequest, requestList, userInfoDTO.getUid(), jdbcProvider);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", userInfoDTO.getUid().toString());
            jsonObject.put("userId", userInfoDTO.getUid().toString());
            jsonObject.put("userInfo", userList);
            users.add(jsonObject);
            return users;
        }
        if (userInfoDTO.getCountry() != null) {
            List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = getChartFieldCustomFilterDTO("eq", "logic", userInfoDTO.getCountry());
            chartFieldCustomFilterDTO.setField(DatasetTableField.builder().originName("country").deType(DeTypeConstants.DE_STRING).deExtractType(DeTypeConstants.DE_STRING).build());
            requestList.add(chartFieldCustomFilterDTO);
            if (CollectionUtils.isNotEmpty(dw)) {
                startTime.getFilter().get(0).setValue(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"));
                String querySql = qp.getSQLSummaryNoView(productHr + ".ads_new_user_attr_hr", requestList, null, "dt", "desc");
                datasourceRequest.setDatasource(dw.get(0));
                datasourceRequest.setTable(productHr + ".ads_new_user_attr_hr");
                datasourceRequest.setQuery(querySql + " limit 100");
                List<JSONObject> jsonData = jdbcProvider.getJsonRowData(datasourceRequest);
                if (CollectionUtils.isNotEmpty(jsonData)) {
                    for (JSONObject jsonDatum : jsonData) {
                        JSONObject jsonObject = new JSONObject();
                        ArrayList<JSONObject> jsonObjects = new ArrayList<>();
                        jsonObject.put("id", userInfoDTO.getCountry());
                        jsonObject.put("userInfo", jsonObjects);
                        users.add(jsonObject);
                        Set<Map.Entry<String, Object>> entries = jsonDatum.entrySet();
                        for (Map.Entry<String, Object> entry : entries) {
                            JSONObject object = new JSONObject();
                            if (entry.getKey().equals("user_id")) {
                                jsonObject.put("userId", entry.getValue().toString());
                            }
                            object.put("name", entry.getKey());
                            object.put("value", entry.getValue());
                            jsonObjects.add(object);
                        }
                    }
                    return users;
                }
            }
        }
        return users;
    }

    public List<JSONObject> getUserList(String product, String productHr, String tableName, ChartFieldCustomFilterDTO startTime, ImpalaQueryProvider qp, String columnName,
                                        DatasourceRequest datasourceRequest, List<ChartFieldCustomFilterDTO> requestList, Long userId, JdbcProvider jdbcProvider) throws Exception {
        List<JSONObject> users = new ArrayList<>();
        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
        if (CollectionUtils.isNotEmpty(dw)) {
            String querySql = "select " +
                    "*" +
                    " from " + product + "." + tableName + " where dt='" + DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24, "yyyyMMdd") + "' and " + columnName + "=" + userId + " limit 1";
            datasourceRequest.setDatasource(dw.get(0));
            datasourceRequest.setTable(product + "." + tableName);
            datasourceRequest.setQuery(querySql);
            List<JSONObject> jsonData = jdbcProvider.getJsonData(datasourceRequest);
            if (CollectionUtils.isNotEmpty(jsonData)) {
                users = jsonData;
            } else {
                dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), productHr);
                startTime.getFilter().get(0).setValue(DateUtils.getDateString(System.currentTimeMillis(), "yyyyMMdd"));
                querySql = qp.getSQLSummaryNoView(productHr + ".ads_new_user_attr_hr", requestList, null, "dt", "desc");
                datasourceRequest.setDatasource(dw.get(0));
                datasourceRequest.setTable(productHr + ".ads_new_user_attr_hr");
                datasourceRequest.setQuery(querySql + " limit 1");
                jsonData = jdbcProvider.getJsonData(datasourceRequest);
                if (CollectionUtils.isNotEmpty(jsonData)) {
                    users = jsonData;
                } else {
                    dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
                    querySql = "select " +
                            "*" +
                            " from " + product + "." + tableName + " where dt='" + DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24, "yyyyMMdd") + "' and " + columnName + "=" + userId + " limit 1";
                    datasourceRequest.setDatasource(dw.get(0));
                    datasourceRequest.setTable(product + "." + tableName);
                    datasourceRequest.setQuery(querySql);
                    jsonData = jdbcProvider.getJsonData(datasourceRequest);
                    if (CollectionUtils.isNotEmpty(jsonData)) {
                        users = jsonData;
                    }
                }
            }
        }
        return users;
    }

    public ChartFieldCustomFilterDTO getChartFieldCustomFilterDTO(String operate, String type, String value) {
        ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = new ChartFieldCustomFilterDTO();
        ArrayList<ChartCustomFilterItemDTO> chartCustomFilterItemDTOS = new ArrayList<>();
        ChartCustomFilterItemDTO chartCustomFilterItemDTO = new ChartCustomFilterItemDTO();
        chartCustomFilterItemDTOS.add(chartCustomFilterItemDTO);
        chartCustomFilterItemDTO.setTerm(operate);
        chartCustomFilterItemDTO.setValue(value);
        chartFieldCustomFilterDTO.setFilter(chartCustomFilterItemDTOS);
        chartFieldCustomFilterDTO.setFilterType(type);
        return chartFieldCustomFilterDTO;
    }

    public static List<JSONObject> queryPresto(String sql) {
        List<JSONObject> lists = new ArrayList<>();
        Connection con = null;
        Statement stmt = null;
        ResultSet res = null;
        HashMap<String, String> fieldMap = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            con = DriverManager.getConnection("jdbc:presto://monitor13:8077/hive", "dev", "");
            stmt = con.createStatement();
            System.out.println("Running: " + sql);
            res = stmt.executeQuery(sql.replaceAll(";", ""));
            long endTime = System.currentTimeMillis();
            int columnCount = res.getMetaData().getColumnCount();
            while (res.next()) {
                JSONObject jsonObject = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnLabel = res.getMetaData().getColumnLabel(i);
                    jsonObject.put(columnLabel, res.getObject(i));
                }
                lists.add(jsonObject);
            }
            System.out.println(endTime - startTime);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                res.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return lists;
    }
}

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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class UserQueryService {

    @Resource
    private DatasourceService datasourceService;

    private static String micoDw = "mico_dw";

    private static String yohoDw = "yoho_dw";

    private static String micoAl = "mico_al";

    private static String yohoAl = "yoho_al";

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
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
        if (CollectionUtils.isNotEmpty(dw)) {
            String sql = "select event_type,event_name from " + product + ".al_north_star_event_type_mapping where dt='" + DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24, "yyyyMMdd") + "'";
            datasourceRequest.setDatasource(dw.get(0));
            datasourceRequest.setQuery(sql);
            List<JSONObject> jsonData = jdbcProvider.getJsonRowData(datasourceRequest);
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
                        JSONObject o1 = new JSONObject();
                        o1.put("type", type);
                        o1.put("id", object.getString("event_name"));
                        o1.put("label", object.getString("event_name"));
                        children1.add(o1);
                        client.put("children", children1);
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
        return options;
    }

    public JSONObject getUserEventNum(UserInfoDTO userInfoDTO) throws Exception {
        JSONObject event = new JSONObject();
        String product = micoDw;
        if (userInfoDTO.getProduct().equals("Yoho")) {
            product = yohoDw;
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
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
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".dwd_server_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".tl_server_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    break;
                case "client":
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".dwd_client_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".tl_client_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    break;
                case "h5":
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".dwd_h5_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".tl_h5_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    break;
                case "technology":
                    arrayList.add("select dt,event_name,count(*) ct from " + product + ".dwd_apm_client_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") group by dt,event_name");
                    break;
            }
        }
        String sql = String.join(" union all ", arrayList);
        sql = "select dt,event_name,sum(ct) ct from (" + sql + ") t group by dt,event_name order by dt";
        datasourceRequest.setDatasource(dw.get(0));
        datasourceRequest.setQuery(sql);
        List<JSONObject> jsonData = jdbcProvider.getJsonRowData(datasourceRequest);
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
        if (userInfoDTO.getProduct().equals("Yoho")) {
            product = yohoDw;
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
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
                    arrayList.add("select * from " + product + ".dwd_server_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    arrayList.add("select * from " + product + ".tl_server_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    break;
                case "client":
                    arrayList.add("select * from " + product + ".dwd_client_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    arrayList.add("select * from " + product + ".tl_client_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    break;
                case "h5":
                    arrayList.add("select * ct from " + product + ".dwd_h5_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    arrayList.add("select * from " + product + ".tl_h5_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    ;
                    break;
                case "technology":
                    arrayList.add("select * from " + product + ".dwd_apm_client_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + DateUtils.getDateString(userInfoDTO.getStartTime(), "yyyyMMdd") + "' and dt<='" +
                            DateUtils.getDateString(userInfoDTO.getEndTime(), "yyyyMMdd") + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    break;
            }
        }
        String sql = String.join(" union all ", arrayList);
        sql = "select * from (" + sql + ") t order by `timestamp` " + userInfoDTO.getSort();
        datasourceRequest.setDatasource(dw.get(0));
        datasourceRequest.setQuery(sql);
        List<JSONObject> jsonData = jdbcProvider.getJsonRowData(datasourceRequest);
        for (JSONObject object : jsonData) {
            String dt = DateUtils.getDateString(DateUtils.getDate(object.getString("dt"), "yyyyMMdd"));
            Long timestamp = object.getLong("timestamp");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("isShow", false);
            jsonObject.put("name", object.getString("event_name"));
            jsonObject.put("timestamp", DateUtils.getDateString(timestamp, "HH:mm:ss:SSS"));
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
                jsonObject1.put("eventList", jsonObjects);
                eventDetailList.add(jsonObject1);
            }
        }
        return eventDetailList;
    }

    public ArrayList<JSONObject> getUserEventDetailForDate(UserInfoDTO userInfoDTO) throws Exception {
        ArrayList<JSONObject> eventDetailList = new ArrayList<>();
        String product = micoDw;
        if (userInfoDTO.getProduct().equals("Yoho")) {
            product = yohoDw;
        }
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        JdbcProvider jdbcProvider = (JdbcProvider) ProviderFactory.getProvider(DatasourceTypes.impala.getType());
        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
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
                    arrayList.add("select * from " + product + ".dwd_server_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + userInfoDTO.getDate() + "' and dt<='" +
                            userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    arrayList.add("select * from " + product + ".tl_server_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    break;
                case "client":
                    arrayList.add("select * from " + product + ".dwd_client_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + userInfoDTO.getDate() + "' and dt<='" +
                            userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    arrayList.add("select * from " + product + ".tl_client_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    break;
                case "h5":
                    arrayList.add("select * ct from " + product + ".dwd_h5_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + userInfoDTO.getDate() + "' and dt<='" +
                            userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    arrayList.add("select * from " + product + ".tl_h5_event_hr where user_id=" + userInfoDTO.getUserId()
                            + " and dt='" + userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    break;
                case "technology":
                    arrayList.add("select * from " + product + ".dwd_apm_client_event_detail where user_id=" + userInfoDTO.getUserId()
                            + " and dt>='" + userInfoDTO.getDate() + "' and dt<='" +
                            userInfoDTO.getDate() + "' and event_name in (" + String.join(",", entry.getValue()) + ") order by `timestamp`" + userInfoDTO.getSort()
                            + " limit 20 offset " + (userInfoDTO.getPageNo() - 1) * 20);
                    break;
            }
        }
        String sql = String.join(" union all ", arrayList);
        sql = "select * from (" + sql + ") t order by `timestamp` " + userInfoDTO.getSort();
        datasourceRequest.setDatasource(dw.get(0));
        datasourceRequest.setQuery(sql);
        List<JSONObject> jsonData = jdbcProvider.getJsonRowData(datasourceRequest);
        for (JSONObject object : jsonData) {
            String dt = DateUtils.getDateString(DateUtils.getDate(object.getString("dt"), "yyyyMMdd"));
            Long timestamp = object.getLong("timestamp");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("isShow", false);
            jsonObject.put("name", object.getString("event_name"));
            jsonObject.put("timestamp", DateUtils.getDateString(timestamp, "HH:mm:ss:SSS"));
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
                jsonObject1.put("eventList", jsonObjects);
                eventDetailList.add(jsonObject1);
            }
        }
        return eventDetailList;
    }

    public List<JSONObject> getUserList(UserInfoDTO userInfoDTO) throws Exception {
        List<JSONObject> users = new ArrayList<>();
        String productHr = micoDw;
        String product = micoAl;
        if (userInfoDTO.getProduct().equals("Yoho")) {
            product = yohoAl;
            productHr = yohoDw;
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
            List<JSONObject> userList = getUserList(product, productHr, startTime, qp, "user_id", datasourceRequest, requestList, userInfoDTO.getUserId(), jdbcProvider);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", userInfoDTO.getUserId());
            jsonObject.put("userId", userInfoDTO.getUserId());
            jsonObject.put("userInfo", userList);
            users.add(jsonObject);
            return users;
        }
        if (userInfoDTO.getUid() != null) {
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = getChartFieldCustomFilterDTO("eq", "logic", userInfoDTO.getUid().toString());
            chartFieldCustomFilterDTO.setField(DatasetTableField.builder().originName("uid").deType(DeTypeConstants.DE_INT).deExtractType(DeTypeConstants.DE_INT).build());
            requestList.add(chartFieldCustomFilterDTO);
            List<JSONObject> userList = getUserList(product, productHr, startTime, qp, "uid", datasourceRequest, requestList, userInfoDTO.getUid(), jdbcProvider);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", userInfoDTO.getUid());
            jsonObject.put("userId", userInfoDTO.getUid());
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
                String querySql = qp.getSQLSummaryNoView(product + ".ads_new_user_attr_hr", requestList, null, "dt", "desc");
                datasourceRequest.setDatasource(dw.get(0));
                datasourceRequest.setTable(product + ".ads_new_user_attr_hr");
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
                                jsonObject.put("userId", entry.getValue());
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

    public List<JSONObject> getUserList(String product, String productHr, ChartFieldCustomFilterDTO startTime, ImpalaQueryProvider qp, String columnName,
                                        DatasourceRequest datasourceRequest, List<ChartFieldCustomFilterDTO> requestList, Long userId, JdbcProvider jdbcProvider) throws Exception {
        List<JSONObject> users = new ArrayList<>();
        List<Datasource> dw = datasourceService.selectByTypeAndName(DatasourceTypes.impala.getType(), product);
        if (CollectionUtils.isNotEmpty(dw)) {
            String querySql = "select " +
                    "uid,user_id,platform,avatar,photowall,status,gender,user_level,nike_name,birthday,regist_time,country,region,agent_type,gifter_type,gifter_level,gifter_signed_time,vj_type,vj_signed_time,vj_grade,is_new,union_id,union_name,joined_union_time,is_union_head,union_head_uid,family_id,family_name,is_family_head,family_head_uid,broker_id,locale,first_login_device_os,timezone,first_login_account_type,email,phone_number,gold_balance,diamond_balance,silver_balance,wallet_balance,last_login_date,login_days,30_login_days,first_recharge_time,first_recharge_amount,last_recharge_time,last_recharge_amount,recharge_times,recharge_amount,last_media_source,last_media_type,last_install_campaign,last_live_date,is_robot" +
                    " from " + product + ".al_north_star_user_attr where dt='" + DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24, "yyyyMMdd") + "' and " + columnName + "=" + userId + " limit 1";
            datasourceRequest.setDatasource(dw.get(0));
            datasourceRequest.setTable(product + ".al_north_star_user_attr");
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
                            "uid,user_id,platform,avatar,photowall,status,gender,user_level,nike_name,birthday,regist_time,country,region,agent_type,gifter_type,gifter_level,gifter_signed_time,vj_type,vj_signed_time,vj_grade,is_new,union_id,union_name,joined_union_time,is_union_head,union_head_uid,family_id,family_name,is_family_head,family_head_uid,broker_id,locale,first_login_device_os,timezone,first_login_account_type,email,phone_number,gold_balance,diamond_balance,silver_balance,wallet_balance,last_login_date,login_days,30_login_days,first_recharge_time,first_recharge_amount,last_recharge_time,last_recharge_amount,recharge_times,recharge_amount,last_media_source,last_media_type,last_install_campaign,last_live_date,is_robot" +
                            " from " + product + ".al_north_star_user_attr where dt='" + DateUtils.getDateString(System.currentTimeMillis() - 1000 * 60 * 60 * 24, "yyyyMMdd") + "' and " + columnName + "=" + userId + " limit 1";
                    datasourceRequest.setDatasource(dw.get(0));
                    datasourceRequest.setTable(product + ".al_north_star_user_attr");
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
}

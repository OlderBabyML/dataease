package io.dataease.job.sechedule;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.dataease.commons.constants.SysLogConstants;
import io.dataease.commons.utils.CommonBeanFactory;
import io.dataease.commons.utils.DateUtils;
import io.dataease.dto.SysLogDTO;
import io.dataease.dto.chart.ChartViewDTO;
import io.dataease.dto.log.FolderItem;
import io.dataease.plugins.common.base.domain.DatasetTableField;
import io.dataease.plugins.common.base.domain.PanelGroupWithBLOBs;
import io.dataease.plugins.common.base.domain.SchedulerIndexWithBLOBs;
import io.dataease.plugins.common.base.domain.SysUser;
import io.dataease.plugins.common.base.mapper.SchedulerIndexMapper;
import io.dataease.plugins.common.dto.chart.ChartCustomFilterItemDTO;
import io.dataease.plugins.common.dto.chart.ChartFieldCustomFilterDTO;
import io.dataease.plugins.common.dto.chart.ChartViewFieldDTO;
import io.dataease.service.chart.ChartViewService;
import io.dataease.service.dataset.DataSetFieldService;
import io.dataease.service.panel.PanelGroupService;
import io.dataease.service.sys.log.LogService;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class AlarmScheduleJob implements Job {

    private static Gson gson = new Gson();

    private ChartViewService chartViewService;

    private PanelGroupService panelGroupService;

    private SchedulerIndexMapper schedulerIndexMapper;

    private DataSetFieldService dataSetFieldService;

    private LogService logService;

    public AlarmScheduleJob() {
        logService = (LogService) CommonBeanFactory.getBean(LogService.class);
        chartViewService = (ChartViewService) CommonBeanFactory.getBean(ChartViewService.class);
        panelGroupService = (PanelGroupService) CommonBeanFactory.getBean(PanelGroupService.class);
        dataSetFieldService = (DataSetFieldService) CommonBeanFactory.getBean(DataSetFieldService.class);
        schedulerIndexMapper = (SchedulerIndexMapper) CommonBeanFactory.getBean(SchedulerIndexMapper.class);
        DefaultWebSecurityManager manager = new DefaultWebSecurityManager();
        ThreadContext.bind(manager);
    }

    @SneakyThrows
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Gson gson = new Gson();
        //JobKey jobKey = context.getTrigger().getJobKey();
        SysLogDTO sysLogDTO = new SysLogDTO();
        ArrayList<FolderItem> folderItems = new ArrayList<>();
        try {
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            String chartId = jobDataMap.getString("chartId");
            Long id = jobDataMap.getLong("id");
            sysLogDTO.setSourceId(id.toString());
            sysLogDTO.setSourceType(SysLogConstants.SOURCE_TYPE.ALARM.getValue());
            SchedulerIndexWithBLOBs schedulerIndexWithBLOBs = schedulerIndexMapper.selectByPrimaryKey(id);
            ChartViewDTO view = chartViewService.getOne(chartId, null);
            PanelGroupWithBLOBs panel = panelGroupService.findPanelForChartId(view.getId());
            sysLogDTO.setSourceName(panel.getName() + ":" + view.getName() + "指标报警");
            Type tokenType = new TypeToken<ChartFieldCustomFilterDTO>() {
            }.getType();
            Type indexType = new TypeToken<ChartViewFieldDTO>() {
            }.getType();
            Type ruleType = new TypeToken<List<RuleAndSend>>() {
            }.getType();
            long endTimeMillis = System.currentTimeMillis();
            long startimeMillis = 0;
            String timeType = schedulerIndexWithBLOBs.getTimeType();
            Long timeNumber = schedulerIndexWithBLOBs.getTimeNumber();
            switch (timeType) {
                case "秒":
                    startimeMillis = endTimeMillis - timeNumber * 1000;
                    break;
                case "分钟":
                    startimeMillis = endTimeMillis - timeNumber * 1000 * 60;
                    break;
                case "小时":
                    startimeMillis = endTimeMillis - timeNumber * 1000 * 60 * 60;
                    break;
                case "天":
                    startimeMillis = endTimeMillis - timeNumber * 1000 * 60 * 60 * 24;
                    break;
            }
            ChartFieldCustomFilterDTO chartFieldCustomFilterDTO = gson.fromJson(schedulerIndexWithBLOBs.getTimeField(), tokenType);
            ArrayList<ChartCustomFilterItemDTO> chartCustomFilterItemDTOS = new ArrayList<>();
            ChartCustomFilterItemDTO chartCustomFilterItemDTO = new ChartCustomFilterItemDTO();
            chartCustomFilterItemDTOS.add(chartCustomFilterItemDTO);
            chartCustomFilterItemDTO.setFieldId(chartFieldCustomFilterDTO.getId());
            chartCustomFilterItemDTO.setTerm("ge");
            chartCustomFilterItemDTO.setValue(DateUtils.getDateString(startimeMillis, schedulerIndexWithBLOBs.getFormat()));

            ChartCustomFilterItemDTO chartCustomFilterItemDTO1 = new ChartCustomFilterItemDTO();
            chartCustomFilterItemDTOS.add(chartCustomFilterItemDTO1);
            chartCustomFilterItemDTO1.setFieldId(chartFieldCustomFilterDTO.getId());
            chartCustomFilterItemDTO1.setTerm("le");
            chartCustomFilterItemDTO1.setValue(DateUtils.getDateString(endTimeMillis, schedulerIndexWithBLOBs.getFormat()));

            ChartFieldCustomFilterDTO fieldCustomFilterTypeTime = gson.fromJson(schedulerIndexWithBLOBs.getTimeField(), tokenType);
            fieldCustomFilterTypeTime.setFilter(chartCustomFilterItemDTOS);

            ChartViewFieldDTO index = gson.fromJson(schedulerIndexWithBLOBs.getIndexField(), indexType);
            List<RuleAndSend> rules = gson.fromJson(schedulerIndexWithBLOBs.getRules(), ruleType);
            List<RuleAndSend> sends = gson.fromJson(schedulerIndexWithBLOBs.getSends(), ruleType);
            ArrayList<String> msg = new ArrayList<>();
            for (RuleAndSend rule : rules) {
                Type ruleType1 = new TypeToken<RuleAndSend>() {
                }.getType();
                RuleAndSend send = gson.fromJson(rule.getSend().toString(), ruleType1);
                String operate = rule.getOperate();
                String type = rule.getType();
                String condition = rule.getCondition();
                String conditionValue = rule.getConditionValue();
                String ruleDatabaseName = null;
                ChartFieldCustomFilterDTO fieldCustomFilterType = new ChartFieldCustomFilterDTO();
                Type tokenTypes = new TypeToken<List<ChartFieldCustomFilterDTO>>() {
                }.getType();
                Type xFields = new TypeToken<List<ChartViewFieldDTO>>() {
                }.getType();
                List<ChartViewFieldDTO> xAxis = gson.fromJson(view.getXAxis(), xFields);
                List<ChartFieldCustomFilterDTO> fieldCustomFilters = gson.fromJson(view.getCustomFilter(), tokenTypes);
                List<ChartFieldCustomFilterDTO> fieldCustomFilter = new ArrayList<>();
                if (!StringUtils.isEmpty(condition) && !StringUtils.isEmpty(conditionValue)) {
                    DatasetTableField dataSetField = dataSetFieldService.getDataSetField(condition);

                    ArrayList<ChartCustomFilterItemDTO> filterItemDTOS = new ArrayList<>();
                    ChartCustomFilterItemDTO chartCustomFilterItemDTO2 = new ChartCustomFilterItemDTO();
                    chartCustomFilterItemDTO2.setFieldId(condition);
                    chartCustomFilterItemDTO2.setTerm("in");
                    chartCustomFilterItemDTO2.setValue(conditionValue);
                    filterItemDTOS.add(chartCustomFilterItemDTO2);

                    fieldCustomFilterType.setId(dataSetField.getId());
                    fieldCustomFilterType.setField(dataSetField);
                    fieldCustomFilterType.setFilter(filterItemDTOS);
                    fieldCustomFilter.add(fieldCustomFilterType);

                    ChartViewFieldDTO chartViewFieldDTO = new ChartViewFieldDTO();
                    chartViewFieldDTO.setOriginName(dataSetField.getOriginName());
                    chartViewFieldDTO.setDataeaseName(dataSetField.getDataeaseName());
                    chartViewFieldDTO.setDeExtractType(dataSetField.getDeExtractType());
                    chartViewFieldDTO.setDeType(dataSetField.getDeType());
                    xAxis.add(chartViewFieldDTO);
                    view.setXAxis(JSONObject.toJSONString(xAxis));

                    ruleDatabaseName = dataSetField.getDataeaseName();


                }
                fieldCustomFilter.add(fieldCustomFilterTypeTime);
                fieldCustomFilter.addAll(fieldCustomFilters);
                ArrayList<String> ruleNames = new ArrayList<>();
                for (ChartViewFieldDTO xAxi : xAxis) {
                    if (!xAxi.getDataeaseName().equals(chartFieldCustomFilterDTO.getDataeaseName())) {
                        ruleNames.add(xAxi.getDataeaseName());
                    }
                }
                List<ChartViewFieldDTO> xAxisExt = gson.fromJson(view.getXAxisExt(), xFields);
                for (ChartViewFieldDTO chartViewFieldDTO : xAxisExt) {
                    ruleNames.add(chartViewFieldDTO.getDataeaseName());
                }
                if (ruleNames.size() > 0) {
                    ruleDatabaseName = org.apache.commons.lang.StringUtils.join(ruleNames, ",");
                }
                ArrayList<HashMap> tableRow = (ArrayList) chartViewService.calcData(view, false, fieldCustomFilter, null).getData().get("tableRow");
                BigDecimal value = new BigDecimal(rule.getValue());
                switch (operate) {
                    case "高于":
                        if (type.equals("固定值")) {
                            for (HashMap hashMap : tableRow) {
                                ArrayList<String> indexNameList = new ArrayList<>();
                                for (Object o : hashMap.keySet()) {
                                    if (!o.toString().equals(index.getDataeaseName())) {
                                        indexNameList.add(hashMap.get(o.toString()).toString());
                                    }
                                }
                                BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                if (o.compareTo(value) > 0) {
                                    FolderItem folderItem = new FolderItem();
                                    folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,值为:" + o + "（高于设定的值" + value + "）");
                                    folderItems.add(folderItem);
                                    boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,值为:" + o + "（高于设定的值" + value + "）");
                                    if (!b) {
                                        msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,值为:" + o + "（高于设定的值" + value + "）");
                                    }
                                }
                            }
                        } else if (type.equals("环比过去N天平均值")) {
                            Integer numDay = rule.getNumDay();
                            HashMap<String, BigDecimal> avgDataMap = getAvgData(index.getDataeaseName(), ruleDatabaseName, schedulerIndexWithBLOBs.getTimeField(), view, numDay,
                                    chartFieldCustomFilterDTO.getId(), startimeMillis, startimeMillis, fieldCustomFilters, fieldCustomFilterType, schedulerIndexWithBLOBs.getFormat());
                            if (avgDataMap == null || avgDataMap.isEmpty()) {
                                continue;
                            }
                            if (ruleDatabaseName != null) {
                                for (HashMap hashMap : tableRow) {
                                    ArrayList<String> indexNameList = new ArrayList<>();
                                    for (Object o : hashMap.keySet()) {
                                        if (!o.toString().equals(index.getDataeaseName()) && ruleDatabaseName.contains(o.toString())) {
                                            indexNameList.add(hashMap.get(o.toString()).toString());
                                        }
                                    }
                                    BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                    BigDecimal bigDecimal = avgDataMap.get(StringUtils.join(indexNameList, ","));
                                    if (bigDecimal == null || bigDecimal.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal avg = bigDecimal.divide(new BigDecimal(numDay), 8, RoundingMode.HALF_UP);
                                    if (avg.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal decimal = o.add(avg.multiply(new BigDecimal(-1))).divide(avg, 8, RoundingMode.HALF_UP);
                                    if (decimal.compareTo(value) > 0) {
                                        FolderItem folderItem = new FolderItem();
                                        folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下环比" + numDay + "日均值为:" + decimal + "（高于设定的值" + value + "）");
                                        folderItems.add(folderItem);
                                        boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下环比" + numDay + "日均值为:" + decimal + "（高于设定的值" + value + "）");
                                        if (!b) {
                                            msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下环比" + numDay + "日均值为:" + decimal + "（高于设定的值" + value + "）");
                                        }
                                    }
                                }
                            } else {
                                for (HashMap hashMap : tableRow) {
                                    BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                    BigDecimal bigDecimal = avgDataMap.get("date");
                                    BigDecimal avg = bigDecimal.divide(new BigDecimal(numDay), 8, RoundingMode.HALF_UP);
                                    if (avg.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal decimal = o.add(avg.multiply(new BigDecimal(-1))).divide(avg, 8, RoundingMode.HALF_UP);
                                    if (decimal.compareTo(value) > 0) {
                                        FolderItem folderItem = new FolderItem();
                                        folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标环比" + numDay + "日均值为:" + decimal + "（高于设定的值" + value + "）");
                                        folderItems.add(folderItem);
                                        boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标环比" + numDay + "日均值为:" + decimal + "（高于设定的值" + value + "）");
                                        if (!b) {
                                            msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标环比" + numDay + "日均值为:" + decimal + "（高于设定的值" + value + "）");
                                        }
                                    }
                                }
                            }
                        } else if (type.equals("同比上月同期")) {
                            HashMap<String, BigDecimal> avgDataMap = getMonthData(index.getDataeaseName(), ruleDatabaseName, schedulerIndexWithBLOBs.getTimeField(), view,
                                    chartFieldCustomFilterDTO.getId(), startimeMillis, startimeMillis, fieldCustomFilters, fieldCustomFilterType, schedulerIndexWithBLOBs.getFormat());
                            if (avgDataMap == null || avgDataMap.isEmpty()) {
                                continue;
                            }
                            if (ruleDatabaseName != null) {
                                for (HashMap hashMap : tableRow) {
                                    ArrayList<String> indexNameList = new ArrayList<>();
                                    for (Object o : hashMap.keySet()) {
                                        if (!o.toString().equals(index.getDataeaseName()) && ruleDatabaseName.contains(o.toString())) {
                                            indexNameList.add(hashMap.get(o.toString()).toString());
                                        }
                                    }
                                    BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                    BigDecimal bigDecimal = avgDataMap.get(StringUtils.join(indexNameList, ","));
                                    if (bigDecimal == null || bigDecimal.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal avg = o.divide(bigDecimal, 8, RoundingMode.HALF_UP);
                                    if (avg.compareTo(value) > 0) {
                                        FolderItem folderItem = new FolderItem();
                                        folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,上月同比值为:" + avg + "（高于设定的值" + value + "）");
                                        folderItems.add(folderItem);
                                        boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,上月同比值为:" + avg + "（高于设定的值" + value + "）");
                                        if (!b) {
                                            msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,上月同比值为:" + avg + "（高于设定的值" + value + "）");
                                        }
                                    }
                                }
                            } else {
                                for (HashMap hashMap : tableRow) {
                                    BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                    BigDecimal bigDecimal = avgDataMap.get("date");
                                    if (bigDecimal != null && bigDecimal.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal avg = o.divide(bigDecimal, 8, RoundingMode.HALF_UP);
                                    if (avg.compareTo(value) > 0) {
                                        FolderItem folderItem = new FolderItem();
                                        folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标,上月同比值为:" + avg + "（高于设定的值" + value + "）");
                                        folderItems.add(folderItem);
                                        boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标,上月同比值为:" + avg + "（高于设定的值" + value + "）");
                                        if (!b) {
                                            msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标,上月同比值为:" + avg + "（高于设定的值" + value + "）");
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case "低于":
                        if (type.equals("固定值")) {
                            for (HashMap hashMap : tableRow) {
                                ArrayList<String> indexNameList = new ArrayList<>();
                                for (Object o : hashMap.keySet()) {
                                    if (!o.toString().equals(index.getDataeaseName())) {
                                        indexNameList.add(hashMap.get(o.toString()).toString());
                                    }
                                }
                                BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                if (o.compareTo(value) < 0) {
                                    FolderItem folderItem = new FolderItem();
                                    folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,值为:" + o + "（低于设定的值" + value + "）");
                                    folderItems.add(folderItem);
                                    boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,值为:" + o + "（低于设定的值" + value + "）");
                                    if (!b) {
                                        msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,值为:" + o + "（低于设定的值" + value + "）");
                                    }
                                }
                            }
                        } else if (type.equals("环比过去N天平均值")) {
                            Integer numDay = rule.getNumDay();
                            HashMap<String, BigDecimal> avgDataMap = getAvgData(index.getDataeaseName(), ruleDatabaseName, schedulerIndexWithBLOBs.getTimeField(), view, numDay,
                                    chartFieldCustomFilterDTO.getId(), startimeMillis, startimeMillis, fieldCustomFilters, fieldCustomFilterType, schedulerIndexWithBLOBs.getFormat());
                            if (avgDataMap == null || avgDataMap.isEmpty()) {
                                continue;
                            }
                            if (ruleDatabaseName != null) {
                                for (HashMap hashMap : tableRow) {
                                    ArrayList<String> indexNameList = new ArrayList<>();
                                    for (Object o : hashMap.keySet()) {
                                        if (!o.toString().equals(index.getDataeaseName()) && ruleDatabaseName.contains(o.toString())) {
                                            indexNameList.add(hashMap.get(o.toString()).toString());
                                        }
                                    }
                                    BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                    BigDecimal bigDecimal = avgDataMap.get(StringUtils.join(indexNameList, ","));
                                    if (bigDecimal == null || bigDecimal.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal avg = bigDecimal.divide(new BigDecimal(numDay), 8, RoundingMode.HALF_UP);
                                    if (avg.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal decimal = o.add(avg.multiply(new BigDecimal(-1))).divide(avg, 8, RoundingMode.HALF_UP);
                                    if (decimal.compareTo(value) < 0) {
                                        FolderItem folderItem = new FolderItem();
                                        folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下环比" + numDay + "日均值为:" + decimal + "（低于设定的值" + value + "）");
                                        folderItems.add(folderItem);
                                        boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下环比" + numDay + "日均值为:" + decimal + "（低于设定的值" + value + "）");
                                        if (!b) {
                                            msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下环比" + numDay + "日均值为:" + decimal + "（低于设定的值" + value + "）");
                                        }
                                    }
                                }
                            } else {
                                for (HashMap hashMap : tableRow) {
                                    BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                    BigDecimal bigDecimal = avgDataMap.get("date");
                                    BigDecimal avg = bigDecimal.divide(new BigDecimal(numDay), 8, RoundingMode.HALF_UP);
                                    if (avg.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal decimal = o.add(avg.multiply(new BigDecimal(-1))).divide(avg, 8, RoundingMode.HALF_UP);
                                    if (decimal.compareTo(value) < 0) {
                                        FolderItem folderItem = new FolderItem();
                                        folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标环比" + numDay + "日均值为:" + decimal + "（低于设定的值" + value + "）");
                                        folderItems.add(folderItem);
                                        boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标环比" + numDay + "日均值为:" + decimal + "（低于设定的值" + value + "）");
                                        if (!b) {
                                            msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标环比" + numDay + "日均值为:" + decimal + "（低于设定的值" + value + "）");
                                        }
                                    }
                                }
                            }
                        } else if (type.equals("同比上月同期")) {
                            HashMap<String, BigDecimal> avgDataMap = getMonthData(index.getDataeaseName(), ruleDatabaseName, schedulerIndexWithBLOBs.getTimeField(), view,
                                    chartFieldCustomFilterDTO.getId(), startimeMillis, startimeMillis, fieldCustomFilters, fieldCustomFilterType, schedulerIndexWithBLOBs.getFormat());
                            if (avgDataMap == null || avgDataMap.isEmpty()) {
                                continue;
                            }
                            if (ruleDatabaseName != null) {
                                for (HashMap hashMap : tableRow) {
                                    ArrayList<String> indexNameList = new ArrayList<>();
                                    for (Object o : hashMap.keySet()) {
                                        if (!o.toString().equals(index.getDataeaseName()) && ruleDatabaseName.contains(o.toString())) {
                                            indexNameList.add(hashMap.get(o.toString()).toString());
                                        }
                                    }
                                    BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                    BigDecimal bigDecimal = avgDataMap.get(StringUtils.join(indexNameList, ","));
                                    if (bigDecimal == null || bigDecimal.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal avg = o.divide(bigDecimal, 8, RoundingMode.HALF_UP);
                                    if (avg.compareTo(value) < 0) {
                                        FolderItem folderItem = new FolderItem();
                                        folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,上月同比值为:" + avg + "（低于设定的值" + value + "）");
                                        folderItems.add(folderItem);
                                        boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,上月同比值为:" + avg + "（低于设定的值" + value + "）");
                                        if (!b) {
                                            msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标在" + StringUtils.join(indexNameList, ",").replaceAll("\\s+", "_") + "下,上月同比值为:" + avg + "（低于设定的值" + value + "）");
                                        }
                                    }
                                }
                            } else {
                                for (HashMap hashMap : tableRow) {
                                    BigDecimal o = (BigDecimal) hashMap.get(index.getDataeaseName());
                                    BigDecimal bigDecimal = avgDataMap.get("date");
                                    if (bigDecimal != null && bigDecimal.toString().equals("0")) {
                                        continue;
                                    }
                                    BigDecimal avg = o.divide(bigDecimal, 8, RoundingMode.HALF_UP);
                                    if (avg.compareTo(value) < 0) {
                                        FolderItem folderItem = new FolderItem();
                                        folderItem.setName(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标,上月同比值为:" + avg + "（低于设定的值" + value + "）");
                                        folderItems.add(folderItem);
                                        boolean b = sendAlarm(send, panel, view, hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标,上月同比值为:" + avg + "（低于设定的值" + value + "）");
                                        if (!b) {
                                            msg.add(hashMap.get(chartFieldCustomFilterDTO.getDataeaseName()).toString() + ":指标,上月同比值为:" + avg + "（低于设定的值" + value + "）");
                                        }
                                    }
                                }
                            }
                        }
                        break;
                }
            }
            if (msg.size() > 0) {
                for (RuleAndSend send : sends) {
                    String type = send.getType();
                    List<SysUser> users = send.getUsers();
                    String link = send.getLink();
                    switch (type) {
                        case "邮箱":
                            for (SysUser user : users) {
                                Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                                        + " -t mail -u " + user.getUsername()
                                        + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                                        + " -m " + StringUtils.join(msg, ","));
                            }
                            break;
                        case "飞书个人":
                            for (SysUser user : users) {
                                Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                                        + " -t person -u " + user.getNickName()
                                        + " -i " + user.getToken()
                                        + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                                        + " -m " + StringUtils.join(msg, ","));
                            }
                            break;
                        case "电话":
                            for (SysUser user : users) {
                                String[] cmd = {"sh", "-c", "sh " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "callPhone.sh"
                                        + " " + user.getPhone().substring(3)
                                        + " '" + panel.getName() + ":" + view.getName() + "指标报警" + StringUtils.join(msg, ",") + "'"};
                                Runtime.getRuntime().exec(cmd);
                            }
                            break;
                        case "飞书群组":
                            Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                                    + " -t group -H " + link
                                    + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                                    + " -m " + StringUtils.join(msg, ","));
                            break;
                    }
                }
            }
            FolderItem folderItem = new FolderItem();
            folderItem.setName("执行成功");
            sysLogDTO.setRemarks(folderItems);
            sysLogDTO.setOperateType(SysLogConstants.OPERATE_TYPE.SUCCESS.getValue());
            logService.saveAlarmLog(sysLogDTO);
        } catch (Exception e) {
            e.printStackTrace();
            FolderItem folderItem = new FolderItem();
            folderItem.setName(e.getMessage());
            sysLogDTO.setRemarks(folderItems);
            sysLogDTO.setOperateType(SysLogConstants.OPERATE_TYPE.FAILED.getValue());
            logService.saveAlarmLog(sysLogDTO);
        }
    }

    public boolean sendAlarm(RuleAndSend send, PanelGroupWithBLOBs panel, ChartViewDTO view, String msg) throws IOException {
        String type = send.getType();
        List<SysUser> users = send.getUsers();
        String link = send.getLink();
        if (users == null && (link == null || link.equals(""))) {
            return false;
        }
        if (type == null) {
            return false;
        }
        switch (type) {
            case "邮箱":
                for (SysUser user : users) {
                    Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                            + " -t mail -u " + user.getUsername()
                            + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                            + " -m " + msg);
                }
                break;
            case "飞书个人":
                for (SysUser user : users) {
                    Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                            + " -t person -u " + user.getNickName()
                            + " -i " + user.getToken()
                            + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                            + " -m " + msg);
                }
                break;
            case "电话":
                for (SysUser user : users) {
                    String[] cmd = {"sh", "-c", "sh " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "callPhone.sh"
                            + " " + user.getPhone().substring(3)
                            + " '" + panel.getName() + ":" + view.getName() + "指标报警" + msg + "'"};
                    Runtime.getRuntime().exec(cmd);
                }
                break;
            case "飞书群组":
                Runtime.getRuntime().exec("python " + File.separator + "data" + File.separator + "data-platform" + File.separator + "tools" + File.separator + "index_alarm.py"
                        + " -t group -H " + link
                        + " -s " + panel.getName() + ":" + view.getName() + "指标报警"
                        + " -m " + msg);
                break;
        }
        return true;
    }

    public HashMap<String, BigDecimal> getAvgData(String index, String ruleDatabaseName, String timeField, ChartViewDTO view, Integer numDay, String fieldId, Long startimeMillis, Long endTimeMillis, List<ChartFieldCustomFilterDTO> fieldCustomFilters, ChartFieldCustomFilterDTO condition, String format) throws Exception {
        int i = 1;
        HashMap<String, BigDecimal> map = new HashMap<>();
        while (i <= numDay) {
            ArrayList<ChartCustomFilterItemDTO> chartCustomFilterItemDTOList = new ArrayList<>();
            ChartCustomFilterItemDTO chartCustom = new ChartCustomFilterItemDTO();
            chartCustomFilterItemDTOList.add(chartCustom);
            chartCustom.setFieldId(fieldId);
            chartCustom.setTerm("ge");
            chartCustom.setValue(DateUtils.getDateString(startimeMillis - i * 1000 * 60 * 60 * 24, format));

            ChartCustomFilterItemDTO chartCustom1 = new ChartCustomFilterItemDTO();
            chartCustomFilterItemDTOList.add(chartCustom1);
            chartCustom1.setFieldId(fieldId);
            chartCustom1.setTerm("le");
            chartCustom1.setValue(DateUtils.getDateString(endTimeMillis - i * 1000 * 60 * 60 * 24, format));

            Type filterTokenType = new TypeToken<ChartFieldCustomFilterDTO>() {
            }.getType();
            ChartFieldCustomFilterDTO fieldCustomFilterType = gson.fromJson(timeField, filterTokenType);
            fieldCustomFilterType.setFilter(chartCustomFilterItemDTOList);
            List<ChartFieldCustomFilterDTO> fieldCustomFilter = new ArrayList<>();
            fieldCustomFilter.add(fieldCustomFilterType);
            fieldCustomFilter.add(condition);
            fieldCustomFilter.addAll(fieldCustomFilters);

            ChartViewDTO chartView = chartViewService.calcData(view, false, fieldCustomFilter, null);
            ArrayList<HashMap> row = (ArrayList) chartView.getData().get("tableRow");
            for (HashMap hashMap : row) {
                BigDecimal o = (BigDecimal) hashMap.get(index);
                if (ruleDatabaseName != null) {
                    ArrayList<String> indexNameList = new ArrayList<>();
                    for (Object o1 : hashMap.keySet()) {
                        if (!o1.toString().equals(index) && ruleDatabaseName.contains(o1.toString())) {
                            indexNameList.add(hashMap.get(o1.toString()).toString());
                        }
                    }
                    if (map.containsKey(StringUtils.join(indexNameList, ","))) {
                        map.put(StringUtils.join(indexNameList, ","), map.get(StringUtils.join(indexNameList, ",")).add(o));
                    } else {
                        map.put(StringUtils.join(indexNameList, ","), o);
                    }
                } else {
                    if (map.containsKey("date")) {
                        map.put("date", map.get("date").add(o));
                    } else {
                        map.put("date", o);
                    }
                }
            }
            i = i + 1;
        }
        return map;
    }

    public HashMap<String, BigDecimal> getMonthData(String index, String ruleDatabaseName, String timeField, ChartViewDTO view, String fieldId, Long startimeMillis, Long endTimeMillis, List<ChartFieldCustomFilterDTO> fieldCustomFilters, ChartFieldCustomFilterDTO condition, String format) throws Exception {
        int i = 1;
        HashMap<String, BigDecimal> map = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(startimeMillis));
        calendar.add(Calendar.MONTH, -1);

        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(new Date(endTimeMillis));
        calendar1.add(Calendar.MONTH, -1);

        ArrayList<ChartCustomFilterItemDTO> chartCustomFilterItemDTOList = new ArrayList<>();
        ChartCustomFilterItemDTO chartCustom = new ChartCustomFilterItemDTO();
        chartCustomFilterItemDTOList.add(chartCustom);
        chartCustom.setFieldId(fieldId);
        chartCustom.setTerm("ge");
        chartCustom.setValue(DateUtils.getDateString(calendar.getTime().getTime(), format));

        ChartCustomFilterItemDTO chartCustom1 = new ChartCustomFilterItemDTO();
        chartCustomFilterItemDTOList.add(chartCustom1);
        chartCustom1.setFieldId(fieldId);
        chartCustom1.setTerm("le");
        chartCustom1.setValue(DateUtils.getDateString(calendar1.getTime().getTime(), format));

        Type filterTokenType = new TypeToken<ChartFieldCustomFilterDTO>() {
        }.getType();
        ChartFieldCustomFilterDTO fieldCustomFilterType = gson.fromJson(timeField, filterTokenType);
        fieldCustomFilterType.setFilter(chartCustomFilterItemDTOList);
        List<ChartFieldCustomFilterDTO> fieldCustomFilter = new ArrayList<>();
        fieldCustomFilter.add(fieldCustomFilterType);
        fieldCustomFilter.add(condition);
        fieldCustomFilter.addAll(fieldCustomFilters);

        ChartViewDTO chartView = chartViewService.calcData(view, false, fieldCustomFilter, null);
        ArrayList<HashMap> row = (ArrayList) chartView.getData().get("tableRow");
        for (HashMap hashMap : row) {
            BigDecimal o = (BigDecimal) hashMap.get(index);
            if (ruleDatabaseName != null) {
                ArrayList<String> indexNameList = new ArrayList<>();
                for (Object o1 : hashMap.keySet()) {
                    if (!o1.toString().equals(index) && ruleDatabaseName.contains(o1.toString())) {
                        indexNameList.add(hashMap.get(o1.toString()).toString());
                    }
                }
                if (map.containsKey(StringUtils.join(indexNameList, ","))) {
                    map.put(StringUtils.join(indexNameList, ","), map.get(StringUtils.join(indexNameList, ",")).add(o));
                } else {
                    map.put(StringUtils.join(indexNameList, ","), o);
                }
            } else {
                if (map.containsKey("date")) {
                    map.put("date", map.get("date").add(o));
                } else {
                    map.put("date", o);
                }
            }
        }
        return map;
    }

}

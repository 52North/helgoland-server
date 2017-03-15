/*
 * Copyright (C) 2013-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.proxy.connector;

import org.n52.proxy.config.DataSourceConfiguration;
import org.n52.proxy.connector.constellations.MeasurementDatasetConstellation;
import org.n52.proxy.connector.utils.ConnectorHelper;
import org.n52.proxy.connector.utils.DataEntityBuilder;
import org.n52.proxy.connector.utils.EntityBuilder;
import org.n52.proxy.connector.utils.ServiceConstellation;
import org.n52.proxy.db.beans.ProxyServiceEntity;
import org.n52.series.db.beans.CountDatasetEntity;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.MeasurementDatasetEntity;
import org.n52.series.db.beans.TextDatasetEntity;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.shetland.ogc.filter.TemporalFilter;
import org.n52.shetland.ogc.gml.AbstractFeature;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.ows.OwsCapabilities;
import org.n52.shetland.ogc.ows.OwsServiceProvider;
import org.n52.shetland.ogc.ows.service.GetCapabilitiesResponse;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosCapabilities;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.SosObservationOffering;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityRequest;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse;
import org.n52.shetland.ogc.sos.request.GetFeatureOfInterestRequest;
import org.n52.shetland.ogc.sos.request.GetObservationRequest;
import org.n52.shetland.ogc.sos.response.GetFeatureOfInterestResponse;
import org.n52.shetland.ogc.sos.response.GetObservationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configurable
public class SOS2Connector extends AbstractSosConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SOS2Connector.class);

    /**
     * Matches when the provider name is equal "52North" and service version is 2.0.0
     */
    @Override
    protected boolean canHandle(DataSourceConfiguration config, GetCapabilitiesResponse capabilities) {
        OwsCapabilities owsCaps = capabilities.getCapabilities();
        if (owsCaps.getVersion().equals(Sos2Constants.SERVICEVERSION) && owsCaps.getServiceProvider().isPresent()) {
            OwsServiceProvider servProvider = owsCaps.getServiceProvider().get();
            if (servProvider.getProviderName().equals("52North")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ServiceConstellation getConstellation(DataSourceConfiguration config, GetCapabilitiesResponse capabilities) {
        ServiceConstellation serviceConstellation = new ServiceConstellation();
        config.setVersion(Sos2Constants.SERVICEVERSION);
        config.setConnector(getConnectorName());
        ConnectorHelper.addService(config, serviceConstellation);
        SosCapabilities sosCaps = (SosCapabilities) capabilities.getCapabilities();
        addDatasets(serviceConstellation, sosCaps, config.getUrl());
        return serviceConstellation;
    }

    @Override
    public List<DataEntity> getObservations(DatasetEntity seriesEntity, DbQuery query) {
        GetObservationResponse obsResp = createObservationResponse(seriesEntity, ConnectorHelper.createTimePeriodFilter(
                query));
        List<DataEntity> data = new ArrayList<>();
        obsResp.getObservationCollection().forEach((observation) -> {
            data.add(createDataEntity(observation, seriesEntity));
        });
        LOGGER.info("Found " + data.size() + " Entries");
        return data;
    }

    @Override
    public Optional<DataEntity> getFirstObservation(DatasetEntity entity) {
        return createObservationResponse(entity, ConnectorHelper.createFirstTimefilter())
                .getObservationCollection()
                .stream()
                .findFirst()
                .map((obs) -> {
                    return createDataEntity(obs, entity);
                });
    }

    @Override
    public Optional<DataEntity> getLastObservation(DatasetEntity entity) {
        return createObservationResponse(entity, ConnectorHelper.createLatestTimefilter())
                .getObservationCollection()
                .stream()
                .findFirst()
                .map((obs) -> {
                    return createDataEntity(obs, entity);
                });
    }

    @Override
    public UnitEntity getUom(DatasetEntity seriesEntity) {
        GetObservationResponse response = createObservationResponse(seriesEntity,
                ConnectorHelper.createFirstTimefilter());
        if (response.getObservationCollection().size() >= 1) {
            String unit = response.getObservationCollection().get(0).getValue().getValue().getUnit();
            return EntityBuilder.createUnit(unit, (ProxyServiceEntity) seriesEntity.getService());
        }
        return null;
    }

    private DataEntity createDataEntity(OmObservation observation, DatasetEntity seriesEntity) {
        DataEntity dataEntity = null;
        if (seriesEntity instanceof MeasurementDatasetEntity) {
            dataEntity = DataEntityBuilder.createMeasurementDataEntity(observation);
        } else if (seriesEntity instanceof CountDatasetEntity) {
            dataEntity = DataEntityBuilder.createCountDataEntity(observation);
        } else if (seriesEntity instanceof TextDatasetEntity) {
            dataEntity = DataEntityBuilder.createTextDataEntity(observation);
        } else {
            LOGGER.error("No supported datasetEntity for ", seriesEntity);
        }
        return dataEntity;
    }

    protected void addDatasets(ServiceConstellation serviceConstellation, SosCapabilities sosCaps, String serviceUri) {
        sosCaps.getContents().ifPresent((sosObsOfferings) -> {
            sosObsOfferings.forEach((sosObsOff) -> {
                doForOffering(sosObsOff, serviceConstellation, serviceUri);
            });
        });
    }

    protected void doForOffering(SosObservationOffering offering, ServiceConstellation serviceConstellation,
            String serviceUri) {
        String offeringId = ConnectorHelper.addOffering(offering, serviceConstellation);

        offering.getProcedures().forEach((procedureId) -> {
            ConnectorHelper.addProcedure(procedureId, true, false, serviceConstellation);

            GetFeatureOfInterestResponse foiResponse = getFeatureOfInterestResponseByProcedure(procedureId, serviceUri);
            AbstractFeature abstractFeature = foiResponse.getAbstractFeature();
            if (abstractFeature instanceof SamplingFeature) {
                ConnectorHelper.addFeature((SamplingFeature) abstractFeature, serviceConstellation);
            }

            GetDataAvailabilityResponse gdaResponse = getDataAvailabilityResponse(procedureId, serviceUri);
            gdaResponse.getDataAvailabilities().forEach((dataAval) -> {
                String phenomenonId = ConnectorHelper.addPhenomenon(dataAval, serviceConstellation);
                String categoryId = ConnectorHelper.addCategory(dataAval, serviceConstellation);
                String featureId = dataAval.getFeatureOfInterest().getHref();
                // TODO maybe not only MeasurementDatasetConstellation
                serviceConstellation.add(new MeasurementDatasetConstellation(procedureId, offeringId, categoryId,
                        phenomenonId,
                        featureId));
            });

            LOGGER.info(foiResponse.toString());
        });
    }

    protected GetFeatureOfInterestResponse getFeatureOfInterestResponseByProcedure(String procedureId,
                                                                                   String serviceUri) {
        GetFeatureOfInterestRequest request = new GetFeatureOfInterestRequest(SosConstants.SOS,
                Sos2Constants.SERVICEVERSION);
        request.setProcedures(new ArrayList<>(Arrays.asList(procedureId)));
        return (GetFeatureOfInterestResponse) getSosResponseFor(request, Sos2Constants.NS_SOS_20, serviceUri);
    }

    protected GetFeatureOfInterestResponse getFeatureOfInterestResponseByFeature(String featureId, String serviceUri) {
        GetFeatureOfInterestRequest request = new GetFeatureOfInterestRequest(SosConstants.SOS,
                Sos2Constants.SERVICEVERSION);
        request.setFeatureIdentifiers(new ArrayList<>(Arrays.asList(featureId)));
        return (GetFeatureOfInterestResponse) getSosResponseFor(request, Sos2Constants.NS_SOS_20, serviceUri);
    }

    private GetDataAvailabilityResponse getDataAvailabilityResponse(String procedureId, String serviceUri) {
        GetDataAvailabilityRequest request = new GetDataAvailabilityRequest(SosConstants.SOS,
                Sos2Constants.SERVICEVERSION);
        request.setProcedures(new ArrayList<>(Arrays.asList(procedureId)));
        return (GetDataAvailabilityResponse) getSosResponseFor(request, Sos2Constants.NS_SOS_20, serviceUri);
    }

    private GetObservationResponse createObservationResponse(DatasetEntity seriesEntity,
            TemporalFilter temporalFilter) {
        GetObservationRequest request = new GetObservationRequest(SosConstants.SOS, Sos2Constants.SERVICEVERSION);
        request.setProcedures(new ArrayList<>(Arrays.asList(seriesEntity.getProcedure().getDomainId())));
        request.setObservedProperties(new ArrayList<>(Arrays.asList(seriesEntity.getPhenomenon().getDomainId())));
        request.setFeatureIdentifiers(new ArrayList<>(Arrays.asList(seriesEntity.getFeature().getDomainId())));
        request.setTemporalFilters(new ArrayList<>(Arrays.asList(temporalFilter)));
        return (GetObservationResponse) this.getSosResponseFor(request, Sos2Constants.NS_SOS_20,
                seriesEntity.getService().getUrl());
    }

}

/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;

import org.n52.janmayen.function.Functions;
import org.n52.proxy.config.DataSourceConfiguration;
import org.n52.proxy.connector.constellations.QuantityDatasetConstellation;
import org.n52.proxy.connector.utils.EntityBuilder;
import org.n52.proxy.connector.utils.ServiceConstellation;
import org.n52.proxy.connector.utils.ServiceMetadata;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.shetland.ogc.gml.time.TimePeriod;
import org.n52.shetland.ogc.ows.OwsCapabilities;
import org.n52.shetland.ogc.ows.service.GetCapabilitiesResponse;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosCapabilities;
import org.n52.shetland.ogc.sos.SosObservationOffering;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse;
import org.n52.shetland.ogc.sos.response.GetFeatureOfInterestResponse;
import org.n52.shetland.ogc.sos.response.GetObservationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

@Component
@Configurable
public class SOS2Connector extends AbstractSosConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SOS2Connector.class);

    /**
     * Matches when the provider name is equal "52North" and service version is 2.0.0
     *
     * @param config       the config
     * @param response the cababilities
     */
    @Override
    protected boolean canHandle(DataSourceConfiguration config, GetCapabilitiesResponse response) {
        OwsCapabilities capabilities = response.getCapabilities();
        return capabilities.getVersion().equals(Sos2Constants.SERVICEVERSION) &&
               capabilities.getServiceProvider().isPresent() &&
               supportsGDA(capabilities);
    }

    @Override
    public ServiceConstellation getConstellation(DataSourceConfiguration config, GetCapabilitiesResponse capabilities) {
        ServiceConstellation serviceConstellation = new ServiceConstellation();
        config.setVersion(Sos2Constants.SERVICEVERSION);
        config.setConnector(getConnectorName());
        addService(config, serviceConstellation, ServiceMetadata.createXmlServiceMetadata(capabilities.getXmlString()));
        SosCapabilities sosCaps = (SosCapabilities) capabilities.getCapabilities();
        addBindingUrls(sosCaps, config);
        addServiceConfig(config);
        addDatasets(serviceConstellation, sosCaps, config);
        LOGGER.info("{} requests were sended to harvest the service {}", counter, config.getItemName());
        return serviceConstellation;
    }

    @Override
    public List<DataEntity<?>> getObservations(DatasetEntity seriesEntity, DbQuery query) {
        List<DataEntity<?>> data = getObservation(seriesEntity, createTimeFilter(query))
                .getObservationCollection().toStream()
                .map(Functions.currySecond(this::createDataEntity, seriesEntity))
                .collect(toList());
        LOGGER.info("Found {} Entries", data.size());
        return data;
    }

    @Override
    public Optional<DataEntity<?>> getFirstObservation(DatasetEntity dataset) {
        return getObservation(dataset, createFirstTimefilter(dataset))
                .getObservationCollection().toStream().findFirst()
                .map(obs -> createDataEntity(obs, dataset));
    }

    @Override
    public Optional<DataEntity<?>> getLastObservation(DatasetEntity dataset) {
        return getObservation(dataset, createLatestTimefilter(dataset))
                .getObservationCollection().toStream().findFirst()
                .map(obs -> createDataEntity(obs, dataset));
    }

    @Override
    public UnitEntity getUom(DatasetEntity dataset) {
        GetObservationResponse response = getObservation(dataset,
                                                                    createFirstTimefilter(dataset));
        return response.getObservationCollection().toStream()
                .findFirst().map(o -> o.getValue().getValue().getUnit())
                .map(unit -> EntityBuilder.createUnit(unit, null, dataset.getService()))
                .orElse(null);
    }

    protected void addDatasets(ServiceConstellation serviceConstellation, SosCapabilities sosCaps,
                               DataSourceConfiguration config) {

//        if (sosCaps.getContents().isPresent()) {
//            doForOffering(sosCaps.getContents().get().first(), serviceConstellation, config);
//        }
        sosCaps.getContents().ifPresent(contents -> contents
                .forEach(sosObsOff -> doForOffering(sosObsOff, serviceConstellation, config)));
    }

    protected void doForOffering(SosObservationOffering offering, ServiceConstellation serviceConstellation,
                                 DataSourceConfiguration config) {
        LOGGER.debug("Harvest data for offering '{}'", offering.getIdentifier());
        String offeringId = addOffering(offering, serviceConstellation);

        offering.getProcedures().forEach(procedureId -> {
            try {
                addProcedure(procedureId, true, false, serviceConstellation);
                GetFeatureOfInterestResponse foiResponse =
                        getFeatureOfInterestByProcedure(procedureId, config.getUrl());
                addFeature(foiResponse.getAbstractFeature(), serviceConstellation);
                GetDataAvailabilityResponse gdaResponse = getDataAvailabilityByProcedure(procedureId, config.getUrl());
                if (gdaResponse != null) {
                    gdaResponse.getDataAvailabilities().forEach(dataAval -> {
                        String phenomenonId = addPhenomenon(dataAval, serviceConstellation);
                        String categoryId = addCategory(dataAval, serviceConstellation);
                        String featureId = dataAval.getFeatureOfInterest().getHref();
                        TimePeriod phenomenonTime = dataAval.getPhenomenonTime();

                        UnitEntity unit = getUom(procedureId, offeringId, phenomenonId, featureId,
                                serviceConstellation.getService().getSupportsFirstLast(), phenomenonTime.getEnd(),
                                config.getUrl());
                        serviceConstellation.add(new QuantityDatasetConstellation(procedureId, offeringId, categoryId,
                                phenomenonId, featureId, featureId).setUnit(unit)
                                        .setSamplingTimeStart(phenomenonTime.getStart().toDate())
                                        .setSamplingTimeEnd(phenomenonTime.getEnd().toDate()));
                    });
                }
            } catch (Exception e) {
                LOGGER.debug(String.format("Error while processing offering '%s'", offeringId), e);
            }
        });
    }

}

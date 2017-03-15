package org.n52.proxy.connector.constellations;

import org.n52.proxy.connector.utils.EntityBuilder;
import org.n52.proxy.db.beans.ProxyServiceEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.MeasurementDatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.UnitEntity;

import java.util.Date;

/**
 * @author Jan Schulte
 */
public class MeasurementDatasetConstellation extends DatasetConstellation {

    private UnitEntity unit;

    public MeasurementDatasetConstellation(String procedure, String offering, String category, String phenomenon,
            String feature) {
        super(procedure, offering, category, phenomenon, feature);
    }

    public UnitEntity getUnit() {
        return unit;
    }

    public void setUnit(UnitEntity unit) {
        this.unit = unit;
    }

    @Override
    public DatasetEntity createDatasetEntity(ProcedureEntity procedureEntity, CategoryEntity categoryEntity,
                                             FeatureEntity featureEntity, OfferingEntity offeringEntity,
                                             PhenomenonEntity phenomenonEntity, ProxyServiceEntity proxyServiceEntity) {
        MeasurementDatasetEntity measurementDataset = new MeasurementDatasetEntity();
        EntityBuilder.updateDatasetEntity(measurementDataset, procedureEntity, categoryEntity, featureEntity,
                offeringEntity, phenomenonEntity, proxyServiceEntity);
        // add empty unit entity, will be replaced later in the repositories
        if (unit == null) {
            unit = EntityBuilder.createUnit("", proxyServiceEntity);
        }
        measurementDataset.setUnit(unit);
        measurementDataset.setFirstValueAt(new Date());
        measurementDataset.setLastValueAt(new Date());
        return measurementDataset;
    }

}

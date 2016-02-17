library CMSAll version '1'

using QUICK

parameter MeasurementPeriod Interval<DateTime>

context Patient

define InDemographic:
    AgeInYearsAt(start of MeasurementPeriod) >= 2 and AgeInYearsAt(start of MeasurementPeriod) < 18
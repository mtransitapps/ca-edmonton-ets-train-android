package org.mtransit.parser.ca_edmonton_ets_train;

import static org.mtransit.commons.RegexUtils.ANY;
import static org.mtransit.commons.RegexUtils.END;
import static org.mtransit.commons.RegexUtils.WHITESPACE_CAR;
import static org.mtransit.commons.RegexUtils.atLeastOne;
import static org.mtransit.commons.RegexUtils.group;
import static org.mtransit.commons.RegexUtils.groupOr;
import static org.mtransit.commons.RegexUtils.zeroOrMore;
import static org.mtransit.parser.Constants.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.Cleaner;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.regex.Pattern;

// https://data.edmonton.ca/
public class EdmontonETSTrainAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new EdmontonETSTrainAgencyTools().start(args);
	}

	@NotNull
	public String getAgencyName() {
		return "ETS";
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_LIGHT_RAIL;
	}

	private static final String AGENCY_ID = "1"; // Edmonton Transit Service ONLY

	@Nullable
	@Override
	public String getAgencyId() {
		return AGENCY_ID;
	}

	@NotNull
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		//noinspection deprecation
		return gRoute.getRouteId(); // route ID string as route short name used by real-time API  // used by GTFS-RT
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@SuppressWarnings("RedundantMethodOverride")
	@Override
	public boolean useRouteShortNameForRouteId() {
		return false;
	}

	private static final String RSN_CAPITAL_LINE = "Capital";
	private static final String RSN_METRO_LINE = "Metro";
	private static final String RSN_VALLEY_LINE = "Valley";

	private static final Pattern CLEAN_STARTS_LRT = Pattern.compile("(^lrt )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CLEAN_STARTS_LRT.matcher(routeLongName).replaceAll(EMPTY);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	// https://www.edmonton.ca/ets/lrt-station-locations
	private static final String COLOR_CAPITAL_LINE = "4895D0"; // BLUE (from PDF map)
	private static final String COLOR_METRO_LINE = "DB1E32"; // RED (from PDF map)
	private static final String COLOR_VALLEY_LINE = "2B6A3B"; // GREEN (from PDF map)

	@Override
	public @Nullable String provideMissingRouteColor(@NotNull GRoute gRoute) {
		final String rsnS = gRoute.getRouteShortName();
		if (CharUtils.isDigitsOnly(rsnS)) {
			final int rsn = Integer.parseInt(rsnS);
			switch (rsn) {
			case 501: // 21R
				return COLOR_CAPITAL_LINE;
			case 502: // 22R
				return COLOR_METRO_LINE;
			case 503: // 23R
				return COLOR_VALLEY_LINE;
			}
		}
		if (RSN_CAPITAL_LINE.equalsIgnoreCase(rsnS)) {
			return COLOR_CAPITAL_LINE;
		} else if (RSN_METRO_LINE.equalsIgnoreCase(rsnS)) {
			return COLOR_METRO_LINE;
		} else if (RSN_VALLEY_LINE.equalsIgnoreCase(rsnS)) {
			return COLOR_VALLEY_LINE;
		}
		throw new MTLog.Fatal("Unexpected route color for %s!", gRoute.toStringPlus());
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Cleaner STARTS_WITH_LRT_ = new Cleaner(
			group(atLeastOne(ANY) + atLeastOne(WHITESPACE_CAR) + "lrt" + zeroOrMore(WHITESPACE_CAR) + "-" + zeroOrMore(WHITESPACE_CAR)),
			EMPTY,
			true
	);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = STARTS_WITH_LRT_.clean(tripHeadsign);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Cleaner ENDS_WITH_STATION_STOP = new Cleaner(
			group(zeroOrMore(WHITESPACE_CAR) + groupOr("station", "stop") + zeroOrMore(WHITESPACE_CAR) + END),
			true
	);
	private static final Cleaner EDMONTON_ = new Cleaner(
			Cleaner.matchWords("edmonton"),
			"Edm",
			true
	);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = ENDS_WITH_STATION_STOP.matcher(gStopName).replaceAll(EMPTY);
		gStopName = EDMONTON_.clean(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if (!CharUtils.isDigitsOnly(gStop.getStopCode())) {
			throw new MTLog.Fatal("Unexpected stop code %s!", gStop);
		}
		return super.getStopCode(gStop); // used by real-time provider & GTFS-RT
	}
}

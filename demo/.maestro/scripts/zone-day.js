// Works out a calendar day on a fixed-offset clock, for flows that put the end
// user's timezone away from the device's. Maestro runs this on the host.
//
//   - runScript:
//       file: ../scripts/zone-day.js
//       env:
//         OFFSET_HOURS: "14"     # the zone's UTC offset (a zone without daylight saving)
//         DAYS: "-1"             # days from today there (default 0)
//         NAME: kiritimatiYesterday
// The day, as YYYY-MM-DD, is then ${output.kiritimatiYesterday}.
const offsetHours = Number(OFFSET_HOURS);
const days = typeof DAYS === 'string' && DAYS ? Number(DAYS) : 0;
if (!isFinite(offsetHours) || !isFinite(days)) throw new Error('OFFSET_HOURS and DAYS must be numbers');
const local = new Date(Date.now() + offsetHours * 3600 * 1000);
output[NAME] = new Date(Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate() + days)).toISOString().slice(0, 10);
console.log('ZONE-DAY ' + NAME + ' = ' + output[NAME] + ' (UTC' + (offsetHours >= 0 ? '+' : '') + offsetHours + ')');

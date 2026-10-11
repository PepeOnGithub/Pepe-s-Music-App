package xyz.pepe.glacierclient.ecosystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.AddEvent
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.App
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.Calculate
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.CreateNote
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.Open
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.PlayMusic
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.ShowWeather
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.StartTimer
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.Translate
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem.parse

class PepeEcosystemTest {
  @Test fun everyActionRoundTripsThroughItsUri() {
    val actions = listOf(
      PlayMusic("rainy day lo-fi & jazz"),
      Translate("Hola, ¿qué tal?\nSegunda línea", "en"),
      Translate("no target"),
      CreateNote("a=b&c=d 100%", "Lyrics"),
      ShowWeather("São Paulo"),
      ShowWeather(),
      Calculate("(12+3)×4÷2"),
      StartTimer(300, "Tea"),
      AddEvent("Dentist & checkup", 1_800_000_000_000L, "12 High St"),
      AddEvent("Just a title"),
      Open(App.CLOCK),
    )
    actions.forEach { assertEquals(it.toString(), it, parse(it.toUri())) }
  }

  @Test fun rejectsForeignSchemesAndUnknownTargets() {
    assertNull(parse(null))
    assertNull(parse("https://music/play?q=x"))
    assertNull(parse("pepe://spotify/play?q=x"))
    assertNull(parse("pepe://music/delete-everything"))
  }

  @Test fun rejectsEmptyOversizeAndOutOfRangeInput() {
    assertNull(parse("pepe://music/play"))
    assertNull(parse("pepe://music/play?q=%20%20"))
    assertNull(parse("pepe://music/play?q=" + "a".repeat(PepeEcosystem.MAX_QUERY + 1)))
    assertNull(parse("pepe://clock/timer?seconds=0"))
    assertNull(parse("pepe://clock/timer?seconds=999999"))
    assertNull(parse("pepe://clock/timer?seconds=abc"))
    assertNull(parse("pepe://translator/translate?text=" + "a".repeat(PepeEcosystem.MAX_TEXT + 1)))
  }

  @Test fun calculatorOnlyAcceptsMathCharacters() {
    assertNull(parse("pepe://calculator/calc?expr=" + "<script>"))
    assertNull(parse("pepe://calculator/calc?expr=1;rm"))
    assertEquals(Calculate("2^10 + sqrt(9)"), parse(Calculate("2^10 + sqrt(9)").toUri()))
  }

  @Test fun malformedTranslatorTargetIsDroppedNotFatal() {
    assertEquals(Translate("hi", ""), parse("pepe://translator/translate?text=hi&to=../../etc"))
  }

  @Test fun malformedPercentEncodingNeverThrows() {
    assertNull(parse("pepe://music/play?q=%E0%A4%A"))
  }

  @Test fun addEventNeedsSomethingAndSaneTime() {
    assertNull(parse("pepe://calendar/add"))
    assertNull(parse("pepe://calendar/add?start=-5"))
    assertNull(parse("pepe://calendar/add?start=99999999999999999"))
    assertEquals(AddEvent("", 1_700_000_000_000L), parse("pepe://calendar/add?start=1700000000000"))
  }

  @Test fun bareHostMeansOpen() {
    assertEquals(Open(App.WEATHER), parse("pepe://weather"))
    assertEquals(Open(App.NOTES), parse("pepe://notes/open"))
  }
}

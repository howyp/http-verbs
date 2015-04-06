/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.http.reads

import uk.gov.hmrc.play.http._

trait ErrorHttpReads {
  def convertFailuresToExceptions: PartialHttpReads[Nothing] =
    convert400ToBadRequest or
    convert404ToNotFound or
    convert4xxToUpstream4xxResponse or
    convert5xxToUpstream5xxResponse or
    convertLessThan200GreaterThan599ToException

  // TODO not sure that this is a good thing. should be generalised out or more strictly typed
  private def convert(statusMatches: Int => Boolean)(f: (String, String, HttpResponse) => Exception): PartialHttpReads[Nothing] = PartialHttpReads { (method, url, response) =>
    if (statusMatches(response.status)) throw f(method, url, response) else None
  }

  def convert400ToBadRequest = convert(_ == 400) { (m, u, r) => new BadRequestException(s"$m of '$u' returned 400 (Bad Request). Response body '${r.body}'") }
  def convert404ToNotFound = convert(_ == 404) { (m, u, r) => new NotFoundException(s"$m of '$u' returned 404 (Not Found). Response body: '${r.body}'") }
  def convert4xxToUpstream4xxResponse = convert(400 to 499 contains _) { (m, u, r) =>
    new Upstream4xxResponse(s"$m of '$u' returned ${r.status}. Response body: '${r.body}'", r.status, 500, r.allHeaders)
  }
  def convert5xxToUpstream5xxResponse = convert(500 to 599 contains _) { (m, u, r) =>
    new Upstream5xxResponse(s"$m of '$u' returned ${r.status}. Response body: '${r.body}'", r.status, 502)
  }
  def convertLessThan200GreaterThan599ToException = convert(status => status < 200 || status >= 600) { (m, u, r) =>
    new Exception(s"$m to $u failed with status ${r.status}. Response body: '${r.body}'")
  }
}
object ErrorHttpReads extends ErrorHttpReads
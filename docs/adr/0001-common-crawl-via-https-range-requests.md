# Slug discovery reads Common Crawl over HTTPS range requests

Status: accepted

Company slugs are discovered by binary-searching Common Crawl's `cluster.idx` with HTTP
range requests and then range-reading only the matching gzip blocks of the CDX shards, all
over anonymous HTTPS against `data.commoncrawl.org`. This costs about 72 KB of search plus
about 11 MB per crawl for all our URL prefixes, and completes in under a minute.

## Considered options

- **The `index.commoncrawl.org` index server**, which the pre-reset system used. Rejected:
  it failed on 7 of 10 indexes in one production run and 10 of 10 in another, and it
  returns truncated bodies with HTTP 200, `Transfer-Encoding: chunked` and no
  `Content-Length`, so a partial response is undetectable at the HTTP layer and is silently
  parsed as a complete one.
- **The S3 API on `s3://commoncrawl`.** Rejected because anonymous access has been disabled
  since 2022 and returns 403; it would require AWS credentials and buys nothing outside
  us-east-1, and we deploy on our own server.
- **The columnar Parquet index.** Rejected on measurement: the same host query moved 190 MB
  and took 94 seconds, because the files ship without usable column statistics, so a host
  filter degenerates into a full scan of one column across all 300 files.

## Consequences

`cluster.idx` samples only every 3000th record, so reading must begin at the block
*preceding* the first key greater than or equal to the prefix and continue one block past
the last match, or vacancies are silently missed. The CDX index mixes the `warc`,
`robotstxt` and `crawldiagnostics` subsets, so records must be filtered on `/warc/`. SURT
canonicalization uses `webarchive-commons` rather than a hand-rolled implementation, because
a mismatch drops rows without failing. Common Crawl names range requests as the traffic
class hardest for them to serve, so the client backs off on 429 and 503 and identifies
itself.

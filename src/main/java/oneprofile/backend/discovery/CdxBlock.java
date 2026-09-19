package oneprofile.backend.discovery;

/**
 * One compressed block of a CDX shard, as sampled by {@code cluster.idx}: where to find it and how
 * many bytes to ask for.
 *
 * @param shard the name of the CDX shard the block lives in, such as {@code cdx-00199.gz}
 * @param offset the byte offset of the block within that shard
 * @param length the length of the block in bytes
 */
public record CdxBlock(String shard, long offset, int length) {
}

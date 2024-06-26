#ifndef EMBRACE_UNWINDER_DLINFO_H
#define EMBRACE_UNWINDER_DLINFO_H

#include "sampler_unwinder_unwind.h"
#include "../schema/unwind_state.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Uses dladdr to get information on the shared object load address, symbol address, and path
 * for the entire stacktrace.
 * This information may not always be available.
 *
 * See: https://man7.org/linux/man-pages/man3/dladdr.3.html
 */
void emb_symbolicate_stacktrace(emb_sample *sample);

/**
 * Copies stackframes from the unwind_state to the emb_sample struct, applying limits
 * on stacktrace size as necessary. The bottom-most frames will always be preferred
 * (this leads to better flamegraph grouping for traces that exceed the max frame limit).
 */
void emb_copy_frames(emb_sample *sample, const emb_unwind_state *unwind_state);

#ifdef __cplusplus
}
#endif
#endif //EMBRACE_UNWINDER_DLINFO_H

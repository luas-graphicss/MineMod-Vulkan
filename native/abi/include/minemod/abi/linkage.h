#ifndef MINEMOD_ABI_LINKAGE_H
#define MINEMOD_ABI_LINKAGE_H

#ifdef __cplusplus
#define MINEMOD_ABI_BEGIN extern "C" {
#define MINEMOD_ABI_END }
#define MINEMOD_ABI_NOEXCEPT noexcept
#else
#define MINEMOD_ABI_BEGIN
#define MINEMOD_ABI_END
#define MINEMOD_ABI_NOEXCEPT
#endif

#endif

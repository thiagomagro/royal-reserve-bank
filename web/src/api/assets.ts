import { apiFetchJson } from "./client";
import type { AssetAvailability } from "./types";

interface RawAssetAvailability {
  assetCode: string;
  isAssetAvailable?: boolean;
  assetAvailable?: boolean;
}

export function parseAssetAvailability(raw: RawAssetAvailability[]): AssetAvailability[] {
  return raw.map((item) => ({
    assetCode: item.assetCode,
    isAssetAvailable: item.isAssetAvailable ?? item.assetAvailable ?? false,
  }));
}

export async function checkAssetsAvailability(assetCodes: string[]): Promise<AssetAvailability[]> {
  const params = new URLSearchParams();
  for (const code of assetCodes) {
    params.append("assetCode", code);
  }
  const raw = await apiFetchJson<RawAssetAvailability[]>(
    `/api/asset-management?${params.toString()}`,
  );
  return parseAssetAvailability(raw);
}

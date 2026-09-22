import { describe, expect, it } from "vitest";
import { checkAssetsAvailability, parseAssetAvailability } from "./assets";

describe("parseAssetAvailability", () => {
  it("accepts isAssetAvailable", () => {
    expect(parseAssetAvailability([{ assetCode: "PETR4", isAssetAvailable: true }])).toEqual([
      { assetCode: "PETR4", isAssetAvailable: true },
    ]);
  });

  it("accepts assetAvailable (Jackson serialization variant)", () => {
    expect(parseAssetAvailability([{ assetCode: "VALE3", assetAvailable: true }])).toEqual([
      { assetCode: "VALE3", isAssetAvailable: true },
    ]);
    expect(parseAssetAvailability([{ assetCode: "XYZ", assetAvailable: false }])).toEqual([
      { assetCode: "XYZ", isAssetAvailable: false },
    ]);
  });
});

describe("checkAssetsAvailability", () => {
  it("returns availability for requested codes", async () => {
    const results = await checkAssetsAvailability(["PETR4", "UNKNOWN"]);
    expect(results).toEqual([
      { assetCode: "PETR4", isAssetAvailable: true },
      { assetCode: "UNKNOWN", isAssetAvailable: false },
    ]);
  });
});

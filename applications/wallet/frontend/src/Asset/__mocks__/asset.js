const asset = {
  id: 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C',
  metadata: {
    system: {
      jobId: '106d4be1-5d95-11ea-baea-364023b135f5',
      dataSourceId: '10509c20-5d95-11ea-baea-364023b135f5',
      timeCreated: '2020-03-03T21:22:24.922700Z',
      state: 'Analyzed',
      projectId: '89382033-bcbb-4c8f-81a9-3d0883298a81',
      taskId: '106e0f32-5d95-11ea-baea-364023b135f5',
      timeModified: '2020-03-03T21:22:47.575195Z',
    },
    source: {
      path: 'gs://zorroa-dev-data/image/TIFF_1MB.tiff',
      extension: 'tiff',
      filename: 'TIFF_1MB.tiff',
      mimetype: 'image/tiff',
      filesize: 1131930,
      checksum: 1867533868,
      url:
        'https://localhost:3000/api/v1/projects/89382033-bcbb-4c8f-81a9-3d0883298a81/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/files/source/TIFF_1MB.tiff',
    },
    metrics: {
      pipeline: [
        {
          processor: 'boonai_core.core.processors.PreCacheSourceFileProcessor',
          module: 'standard',
          checksum: 1621235190,
          executionTime: 0.11,
          executionDate: '2020-03-03T21:22:27.694149',
        },
        {
          processor: 'boonai_core.image.importers.ImageImporter',
          module: 'standard',
          checksum: 1426657387,
          executionTime: 0.26,
          executionDate: '2020-03-03T21:22:28.215069',
        },
        {
          processor: 'boonai_core.office.importers.OfficeImporter',
          module: 'standard',
          checksum: 2001473853,
          executionTime: 0,
        },
        {
          processor: 'boonai_core.video.VideoImporter',
          module: 'standard',
          checksum: 3310423168,
          executionTime: 0,
        },
        {
          processor: 'boonai_core.core.processors.AssertAttributesProcessor',
          module: 'standard',
          checksum: 1841569083,
          executionTime: 0,
          executionDate: '2020-03-03T21:22:29.654215',
        },
      ],
    },
    media: {
      width: 650,
      height: 434,
      aspect: 1.5,
      orientation: 'landscape',
      type: 'image',
      length: 1,
    },
    clip: {
      type: 'page',
      start: 1,
      stop: 1,
      length: 1,
      pile: 'pUn6wBxUN7x9JxOxLkvruOyNdYA',
      sourceAssetId: 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C',
    },
    files: [
      {
        name: 'image_650x434.jpg',
        category: 'proxy',
        mimetype: 'image/jpeg',
        size: 89643,
        attrs: { width: 650, height: 434 },
        url:
          'https://localhost:3000/api/v1/projects/89382033-bcbb-4c8f-81a9-3d0883298a81/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/files/category/proxy/name/image_650x434.jpg',
      },
      {
        name: 'image_512x341.jpg',
        category: 'proxy',
        mimetype: 'image/jpeg',
        size: 60713,
        attrs: { width: 512, height: 341 },
        url:
          'https://localhost:3000/api/v1/projects/89382033-bcbb-4c8f-81a9-3d0883298a81/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/files/category/proxy/name/image_512x341.jpg',
      },
      {
        name: 'image_320x213.jpg',
        category: 'proxy',
        mimetype: 'image/jpeg',
        size: 30882,
        attrs: { width: 320, height: 213 },
        url:
          'https://localhost:3000/api/v1/projects/89382033-bcbb-4c8f-81a9-3d0883298a81/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/files/category/proxy/name/image_320x213.jpg',
      },
    ],
    analysis: {
      zvi: {
        tinyProxy: [
          '#f3dfc3',
          '#f4efd8',
          '#c18f46',
          '#ebdfbd',
          '#ccd3c0',
          '#e7d4bb',
          '#beae8e',
          '#cabf9e',
          '#d2c09c',
        ],
      },
    },
    similarity: {
      simhash:
        'NMGPEKEACJCHEDDBCHFAPBAHNPPPKMPOPCMODAPNFPCPOPBNEPCPPBHPPCBPDBPJHBGPPPNKOMBHBPNPAHLFCPPPFPGBKPPFDPPHDFAADNKGADBGPFFFGKABGPDLFPPLPPNPPPAAJBPPPLDPKAMCKANPAGCBPBPCEPEAKMHEHPBLKIIFJPOPBPPMGONPPGPFDPCJNPPMPIPPPPHPPCBJPHPGJDNBMGBAPAEBCFDPLPPAJLKANAPPACCPPEAMPADJMCAIPIKNNPDAPMPPCAAOPBADBDJPHGEKFGIDBPHDPPAMPPHLBGPPPHPEPPNAPPPAHOPCLAKPPPFAAHCPFCFPAPBPILPADJAFMJDPCPMCKBBBFBGPLPJMEDDMHPGHMMJPPABAJCAGCFJFNJPBPEPCPCINFEHPHCCPLPGPOIPPMPNIPNDDELAPILCEAPPAIPPDPPBGEFPJLFPJPIECMAEPABALFGLPPLFDPGDABPLMJDPDGBHFPPBPPCPFDPPDFJPAPCDGGGPPGPCPPFIKCHPPPBPLJBPPJIPHLPPKGKDPBCCLNGMAPFHACDINFPPEPBHDLCPGPFPPKPDAJPBEPPPPGPIADNAPABCKHIKPBDLPBPHPLAPFPPDBBEPEHPPPPFBJFANJPMPCFHMDEPDPMNIBDPPAHDBPPNECHLJAIDJFPOLPPBHLABEHPGPPKBAKIPJPBOPPBPHCBIPPAOPIPBAJHBMPPCMADHMPMFKPDEDOPBBAGNNACAADPHDCEKPDPGKHPBFPGJLPPINPPPBPPEANCPPAPBKBFGPAIPEFPFDJGPHAPCPIJHLCBEPPMPPCFPNEPIAPAAEPDHFBMHCNACPFFFDPBLMPPHEMAAIJODGCGCHIGDMAPHDNFEFMHPLPPBAKFBPPAPFPCGJNEDBOBPCLNIIIAEFGAGPGIEAFNPCCCBPKIDIPAPPPPOBPFPADEPPDCECMBFJPAKOEHPBKPGEOBFLNPFAIPPCKPCPHPAIPLPPLEJFFIMICPAPMEPKDNBDOFPJEPCAJEJKEMLHCGENAPEPACPPNPCCLDINPHCDBPHADKIFEPCIPKJIOFOPDEPGEDEPPGPDMBJKPBEHBEPLPFNPJGPJPBPHJHGHPAAEKDIEEAEAGJFPCGLPELPILCAPCFPELIICCPCLPJHCADFIPIGCFLIKPGPJGDCALPBDJPPEGCPLJFGEPBHDPPBPPPIKGDEEDENMBLLIGNECBPEFPBMJKCGPDPJOCNDNLANPKAPPPGPLBBBAJIGEADBEJFFNEGPNKPPPBPGEAELPKLDEGAANIFCLBPPPPNDJGDAFEEACPNEPKKPPKGAHLPDPGPBKPFBDIDDAMPPAIHPHHAPAEGOPCPJEABPPPBAKPLABPCPHNPAEMDPPPPDAJANPPDMPEGFIDEEAAJLGPHPHPCLDPDPFIDCAPPPHIPPPPEDHJDBBPHFKBAOPPPFGEPJPLPCDPBAJCPPDPEFGBFECPIAOGHAPEPAMPNPCPBAPODHNDCIPAGPPPFCPPDMFCLCEHPHAEACACIPCDPPPOMLAKMPPBGDPLFDBMFAPCMIKPHFPPCPPDFPPBPPKPPPPPFECBFIPKKEIPPPPBCKPLEPDEPKCHEPCEJDEKABHBOPPAAPIKABKHBPHPGPDBPHAPJKEDBPBBPLCPFDEEKHEPDFNPGJPAPPOPCCCJPPKPAEDIFPADNADAPDPPEMFPMPPLMPIPIADLPPPFINEPBBBAFBKIFPDPPPEBNPFBEMPFCCEGHEJIMACOCCPHBJPPNKPIPACDPBJPFKPAPIDNPCIGGIPHJGPCKELBLAAPLBBHHHPPCBHPLFAPPJPEEPLHPGPFAOOPPFPJHILPDECMJACKHHAPANLPGALJBNAFODHMJCPPFPJICDPOJBGPKFFPGIJPOHPJAIEPBBKCBIIMIPFPPPAGDDHHHIGGKHFJPPPPEKBGHDCHDPAGEPPIPDPIKALNPHJBGIPPNFINIDPPPEPPCBPPLMFCLGGPDGGLKINGBGKEPHPBPBFHAOBGAHCDGNEBDDCGPDCP',
    },
    labels: [
      {
        label: 'Space',
        modelId: '621bf774-89d9-1244-9596-d6df43f1ede5',
        scope: 'TRAIN',
      },
      {
        label: 'Outside',
        modelId: '621bf774-89d9-9596-1244-d6df43f1ede5',
        scope: 'TRAIN',
      },
    ],
  },
  url:
    'https://localhost:3000/api/v1/projects/89382033-bcbb-4c8f-81a9-3d0883298a81/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/',
}

export default asset

const videoAsset = {
  id: 'srL8ob5cTpCJjYoKkqqfa2ciyG425dGi',
  metadata: {
    system: {
      jobId: 'd6b6c100-3e12-10b8-a559-0244ea53791a',
      dataSourceId: 'd6b6c0ff-3e12-10b8-a559-0244ea53791a',
      timeCreated: '2020-04-30T19:26:06.159197Z',
      state: 'Analyzed',
      projectId: '03aa4218-1243-4a62-8113-42064dcf8f0e',
      timeModified: '2020-04-30T19:32:40.474676Z',
      taskId: 'd6b6c101-3e12-10b8-a559-0244ea53791a',
    },
    files: [
      {
        size: 44507,
        name: 'image_450x360.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/srL8ob5cTpCJjYoKkqqfa2ciyG425dGi/proxy/image_450x360.jpg',
        category: 'proxy',
        attrs: { time_offset: 72.22, width: 450, height: 360 },
      },
      {
        size: 16734,
        name: 'web-proxy.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/srL8ob5cTpCJjYoKkqqfa2ciyG425dGi/web-proxy/web-proxy.jpg',
        category: 'web-proxy',
        attrs: { width: 450, height: 360 },
      },
      {
        size: 26029481,
        name: 'video_450x360.mp4',
        mimetype: 'video/mp4',
        id: 'assets/srL8ob5cTpCJjYoKkqqfa2ciyG425dGi/proxy/video_450x360.mp4',
        category: 'proxy',
        attrs: { frameRate: 25.0, frames: 3611, width: 450, height: 360 },
      },
    ],
    source: {
      path:
        'gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-cypress-testdata/mp4/FatManOnABike1914.mp4',
      extension: 'mp4',
      filename: 'FatManOnABike1914.mp4',
      checksum: 2431356942,
      mimetype: 'video/mp4',
      filesize: 13168719,
    },
    metrics: {
      pipeline: [
        {
          executionTime: 0.56,
          module: 'standard',
          checksum: 2178814325,
          executionDate: '2020-04-30T19:32:24.901160',
          processor: 'boonai_core.core.PreCacheSourceFileProcessor',
        },
        {
          executionTime: 0.49,
          module: 'standard',
          checksum: 117837444,
          executionDate: '2020-04-30T19:32:25.407347',
          processor: 'boonai_core.core.FileImportProcessor',
        },
        {
          executionTime: 0.96,
          module: 'standard',
          checksum: 457707303,
          executionDate: '2020-04-30T19:32:26.387484',
          processor: 'boonai_core.proxy.ImageProxyProcessor',
        },
        {
          executionTime: 6.17,
          module: 'standard',
          checksum: 482873147,
          executionDate: '2020-04-30T19:32:32.573847',
          processor: 'boonai_core.proxy.VideoProxyProcessor',
        },
        {
          executionTime: 0.96,
          module: 'standard',
          checksum: 1879445844,
          executionDate: '2020-04-30T19:32:40.157195',
          processor: 'boonai_analysis.zvi.ZviSimilarityProcessor',
        },
      ],
    },
    media: {
      orientation: 'landscape',
      aspect: 1.25,
      width: 450,
      length: 144.45,
      timeCreated: '2014-01-25T09:28:09.000000Z',
      type: 'video',
      height: 360,
    },
    analysis: {
      'boonai-image-similarity': {
        type: 'similarity',
        simhash:
          'PKCPBJPPBPOLPDIOHJPPOEEDMIPJPPPPAPBGIMFCEGPPPPIPPDMKLPFPNBPPCEBJIPPNPKPMPPPPPPGIPGFPMPKPPFPHPDHPPPLNLPPPFPPPMPPPPJLPPGPJMPPDAPEPIPOBIFKIODPIOPPPNNPPHPPDPPKMPPPPPPJPPFEMKEPEMKEPPPPGPLKPPJPPPHPPCPDPAELPPOPNPFGPPMAFPIDLJIAPPPPPPPPOLPMHNPGPPPGPPPPBPDJPKPPFCPPOFPPPBPDPPPFJAJPKPJPGBPKGEPICPODBJGPPHPGPDPBFJBNLPEPMPPIBPPPPKDNPMPFPPPAPPPPPFJPCIPCPPMPPNPDPFIIPNPPDPPHJBDJPBAPPPFPPINPHJPPPPKPPPPMPGPKPPHPPEPNPPPKKPNPPJPPCMPPPFFPPLLPDLBPPOMJPEBPFPPPPEOIPAFCMLPFPPPMPFLPGIKPDKEPPENPEIPKOPBPEPPIPPPIPMPPBPPPPICPAKPIGCPHIGDNPPPMDBFPJPGLPFPFDPBPCKMPPPMLAEPBPCDOPPPLBGFKPPPPFIGHBPPHDPPGHLJPPPPPPPOCPHBPPPPCJPPBPPKPPPPGDPPPPIPBDIPKPPPPGJPDPOPBPPAMPPPPPFCDPPGPHFODPGEIGJPEPEJCCICPIPPCBPHOPPPOPKGLPPPPPPPPPEEFPPECHDNPALPHLBLLPPPPNPFGIPPPPPPBPPMELPPDPFOCPJPLKCPPOPNHCPHPPPLPJFPDMPPPPKPOPOPKPPAGJGFPNPNBLHPPIPPEHPPPPHPFOPPPHPPPPAPPPPPPFPPJJBPKKEPHCMMPPLLGAKPPPPOPJPOPPOPPPBPIPDPPHMPKFKPLKJCBKEBAJPOHDIPJNICLKPNEILPCAPHPPBMLHBPPDHPGKHAGPHPBPPLPPFPKJPJPPPKPPPBNEHFPLFPNFMPBECDPPFDPJPKPDIPPPHPPHPCNPPPOPPKFFGPPEPNMPPPFPPCDGOPMPLPPPEAHGPFPPIPPFPLPPECIJHOPGEPPDPPJPJIMHPPPPPKPPPEPPPCIPPFPJNPMCPPPPPFPGIAPPDPAFFPFPGPNPPBFGPPGPMPDPMPOPPGGMPGFPOEPPPLPJPHPPPPPPELEJLPPPPPKPLLNGKPPPGPLNOPIMPOCPPPDIPPPPHPNPMIMJKJPPHPJPPPKDPFPGJPEJPIMKDNJPPPDLPPMJPPHPGEDHEPPPPIHAJPHNHPFPNPPPIPJPJPPPFIPFFPCFKPPOFPPMPPPHPIMPPDFJPPDJGNPPPPPPPBLPNBPHHPPDFPIPFIPOPPBGPJBPFPPBBKPPPPLPEMPPMOKMFPOFPFLHPPPPPHPGPGPPPPEEJPPAPPPGPCPFKBOPCPNOMPMKINHCIPPPIBMMLHKJPPEPBPMPNHPAPFPPMPPPPDPPJPPDPALCCLLGEHLPNADPCPBLJKJNOJPPAMPHIPPPMLJALGPKDKPJPPAJPDNPPMPCPPPBPAHPHPLPDPPMPPMGGNPPJDPOCPPCGDCPPPKKFAKPMJBPPPPIDEPPGGPPPPJMLNMHPPHPBPBKDPOPKEKOHPPFOGPMBPCJPFCCPFPPPPPPPNPBPJPMEPPPJPPAOPPHDPPIPIEPPPPPPPKHEPPPPPFPMPPPPGOPGPPPIOPHPMKDDPPBFPHPIPGMLPFEKPCJPPIPPMPPHGMEPPKGPPDPPHPPPPMGPPPEDDEEPOPPPPPPKPBPPPPKPPPPNJKOBPPPPPKBKPPPPPLECIPPPIPFMOPPBPKPBPPPPGPNPPPHPDPHHPIPHKPPPPDPPJHLPEPGDBLCPPDPPDPMPDDPGPPHLPKPPPPPLHEPPPFPPPLPPPGPKPKPPEDPPPPFPPPHPJPPAAPCDPNPPPNILPPPHHPLPOJMPICPJPPHPJBDHFNPPPHPPGKPIPPELPPPPJPPKPLPPPPPPEPPGKPPPAPPEPLPEFHPKPPJPAGPPJPPLDJPONLPPPOBPLGPPPPNPJKADPELNLLPJMIGIFPCPKFGPPPOBPPPCCIDMPEPOPJPHPAPCDPL',
      },
    },
    clip: {
      sourceAssetId: 'srL8ob5cTpCJjYoKkqqfa2ciyG425dGi',
      stop: 144.45,
      pile: '3NCZk9tSd4B6mgZkJYYjOUawMDo',
      start: 0.0,
      length: 144.45000000000002,
      timeline: 'full',
      type: 'scene',
    },
  },
}

export default videoAsset

import { useRouter } from 'next/router'

import { colors, spacing } from '../Styles'

import Panel from '../Panel'
import Assets from '../Assets'
import Filters from '../Filters'
import Metadata from '../Metadata'
import FaceLabeling from '../FaceLabeling'
import Export from '../Export'
import AssetDelete from '../AssetDelete'

import FilterSvg from '../Icons/filter.svg'
import InformationSvg from '../Icons/information.svg'
import UsersSvg from '../Icons/users.svg'
import UploadSvg from '../Icons/upload.svg'
import TrashSvg from '../Icons/trash.svg'

const ICON_WIDTH = 20

let reloadKey = 0

const VisualizerContent = () => {
  const {
    query: { id: assetId, action },
  } = useRouter()

  if (action === 'delete-asset-success') {
    reloadKey += 1
  }

  return (
    <div
      css={{
        height: '100%',
        backgroundColor: colors.structure.coal,
        marginLeft: -spacing.spacious,
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        paddingTop: spacing.hairline,
        display: 'flex',
        flex: 1,
        flexDirection: 'column',
      }}
    >
      <div css={{ display: 'flex', height: '100%', overflowY: 'hidden' }}>
        <Panel openToThe="right">
          {{
            filters: {
              title: 'Filters',
              icon: <FilterSvg width={ICON_WIDTH} aria-hidden />,
              content: <Filters key={reloadKey} />,
            },
          }}
        </Panel>
        <Assets key={reloadKey} />
        <Panel openToThe="left">
          {{
            metadata: {
              title: 'Asset Metadata',
              icon: <InformationSvg width={ICON_WIDTH} aria-hidden />,
              content: <Metadata />,
            },
            faceLabeling: {
              title: 'Add Names & Train',
              icon: <UsersSvg width={ICON_WIDTH} aria-hidden />,
              content: <FaceLabeling />,
            },
            export: {
              title: 'Export',
              icon: (
                <UploadSvg
                  width={ICON_WIDTH}
                  css={{ transform: `rotate(180deg)` }}
                  aria-hidden
                />
              ),
              content: <Export />,
            },
            delete: {
              title: 'Delete',
              icon: <TrashSvg width={ICON_WIDTH} aria-hidden />,
              content: <AssetDelete key={assetId} />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default VisualizerContent

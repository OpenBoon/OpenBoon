import { useRouter } from 'next/router'

import { colors, spacing } from '../Styles'

import Panel from '../Panel'
import Assets from '../Assets'
import Filters from '../Filters'
import Metadata from '../Metadata'
import FaceLabeling from '../FaceLabeling'
import Export from '../Export'
import AssetDelete from '../AssetDelete'
import FiltersIcon from '../Filters/Icon'
import AssetLabeling from '../AssetLabeling'

import InformationSvg from '../Icons/information.svg'
import FaceDetectionSvg from '../Icons/faceDetection.svg'
import UploadSvg from '../Icons/upload.svg'
import TrashSvg from '../Icons/trash.svg'
import PenSvg from '../Icons/pen.svg'

const ICON_SIZE = 20

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
              icon: <FiltersIcon />,
              content: <Filters key={reloadKey} />,
            },
          }}
        </Panel>
        <Assets key={reloadKey} />
        <Panel openToThe="left">
          {{
            metadata: {
              title: 'Asset Metadata',
              icon: <InformationSvg height={ICON_SIZE} />,
              content: <Metadata />,
            },
            faceLabeling: {
              title: 'Face Recognition Training',
              icon: <FaceDetectionSvg height={ICON_SIZE} />,
              content: <FaceLabeling />,
            },
            assetLabeling: {
              title: 'Add Labels To Model',
              icon: <PenSvg height={ICON_SIZE} />,
              content: <AssetLabeling />,
              featureProps: {
                flag: 'asset-labeling',
                envs: [],
              },
            },
            export: {
              title: 'Export',
              icon: (
                <UploadSvg
                  height={ICON_SIZE}
                  css={{ transform: `rotate(180deg)` }}
                />
              ),
              content: <Export />,
            },
            delete: {
              title: 'Delete',
              icon: <TrashSvg height={ICON_SIZE} />,
              content: <AssetDelete key={assetId} />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default VisualizerContent

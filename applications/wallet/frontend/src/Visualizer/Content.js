import { useRouter } from 'next/router'

import { colors, constants, spacing } from '../Styles'

import FetchAhead from '../Fetch/Ahead'
import Panel from '../Panel'
import Assets from '../Assets'
import Filters from '../Filters'
import Metadata from '../Metadata'
import AssetDelete from '../AssetDelete'
import FiltersIcon from '../Filters/Icon'
import AssetLabeling from '../AssetLabeling'

import InformationSvg from '../Icons/information.svg'
import TrashSvg from '../Icons/trash.svg'
import TagsSvg from '../Icons/tags.svg'

let reloadKey = 0

const VisualizerContent = () => {
  const {
    query: { projectId, assetId, action },
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
        <FetchAhead url={`/api/v1/projects/${projectId}/searches/fields/`} />
        <FetchAhead url={`/api/v1/projects/${projectId}/datasets/all/`} />

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
              icon: <InformationSvg height={constants.icons.regular} />,
              content: <Metadata />,
            },
            assetLabeling: {
              title: 'Dataset Labels',
              icon: <TagsSvg height={constants.icons.regular} />,
              content: <AssetLabeling />,
              isBeta: true,
            },
            delete: {
              title: 'Delete',
              icon: <TrashSvg height={constants.icons.regular} />,
              content: <AssetDelete key={assetId} />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default VisualizerContent

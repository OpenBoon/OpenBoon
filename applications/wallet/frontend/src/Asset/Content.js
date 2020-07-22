import { useRouter } from 'next/router'

import { colors, constants, spacing } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
import Panel from '../Panel'
import Metadata from '../Metadata'
import AssetDelete from '../AssetDelete'

import InformationSvg from '../Icons/information.svg'
import TrashSvg from '../Icons/trash.svg'

import AssetAsset from './Asset'
import AssetNavigation from './Navigation'

const AssetContent = () => {
  const {
    query: { projectId, id: assetId, query },
  } = useRouter()

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
      <div
        css={{
          display: 'flex',
          height: '100%',
        }}
      >
        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            width: '100%',
          }}
        >
          <AssetNavigation
            projectId={projectId}
            assetId={assetId}
            query={query}
          />

          <SuspenseBoundary>
            <AssetAsset projectId={projectId} assetId={assetId} />
          </SuspenseBoundary>
        </div>

        <Panel openToThe="left">
          {{
            metadata: {
              title: 'Asset Metadata',
              icon: <InformationSvg height={constants.icons.regular} />,
              content: <Metadata />,
            },
            delete: {
              title: 'Delete',
              icon: <TrashSvg height={constants.icons.regular} />,
              content: <AssetDelete />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default AssetContent

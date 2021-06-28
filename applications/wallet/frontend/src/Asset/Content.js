import { colors, constants, spacing } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
import Panel from '../Panel'
import Metadata from '../Metadata'
import AssetDelete from '../AssetDelete'
import AssetLabeling from '../AssetLabeling'

import InformationSvg from '../Icons/information.svg'
import TrashSvg from '../Icons/trash.svg'
import TagsSvg from '../Icons/tags.svg'

import AssetAsset from './Asset'

const AssetContent = () => {
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
            marginRight: spacing.hairline,
          }}
        >
          <SuspenseBoundary>
            <AssetAsset isQuickView={false} />
          </SuspenseBoundary>
        </div>

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
              content: <AssetDelete />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default AssetContent

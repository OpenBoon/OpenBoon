import Link from 'next/link'
import { useRouter } from 'next/router'

import { constants, colors, spacing, typography, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'
import SuspenseBoundary from '../SuspenseBoundary'
import Panel from '../Panel'
import Metadata from '../Metadata'
import AssetDelete from '../AssetDelete'

import InformationSvg from '../Icons/information.svg'
import CrossSvg from '../Icons/cross.svg'
import TrashSvg from '../Icons/trash.svg'

import AssetAsset from './Asset'

const ICON_SIZE = 20

const AssetContent = () => {
  const {
    query: { projectId, id: assetId, query },
  } = useRouter()

  const idString = `?id=${assetId}`
  const queryString = query ? `&query=${query}` : ''

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
          overflowY: 'hidden',
          position: 'relative',
        }}
      >
        <Link
          href={`/[projectId]/visualizer${idString}${queryString}`}
          as={`/${projectId}/visualizer${idString}${queryString}`}
          passHref
        >
          <Button
            variant={VARIANTS.MENU_ITEM}
            css={{
              position: 'absolute',
              top: spacing.base,
              left: spacing.base,
              padding: spacing.base,
              color: colors.structure.steel,
              borderRadius: constants.borderRadius.small,
              ':hover': {
                color: colors.structure.white,
              },
              zIndex: zIndex.layout.interactive,
            }}
          >
            <div css={{ display: 'flex' }}>
              <CrossSvg height={ICON_SIZE} />

              <span
                css={{
                  paddingLeft: spacing.base,
                  fontSize: typography.size.medium,
                  lineHeight: typography.height.medium,
                  fontWeight: typography.weight.bold,
                  textTransform: 'uppercase',
                }}
              >
                Close View
              </span>
            </div>
          </Button>
        </Link>

        <SuspenseBoundary>
          <AssetAsset projectId={projectId} assetId={assetId} />
        </SuspenseBoundary>

        <Panel openToThe="left">
          {{
            metadata: {
              title: 'Asset Metadata',
              icon: <InformationSvg height={ICON_SIZE} />,
              content: <Metadata />,
            },
            delete: {
              title: 'Delete',
              icon: <TrashSvg height={ICON_SIZE} />,
              content: <AssetDelete />,
            },
          }}
        </Panel>
      </div>
    </div>
  )
}

export default AssetContent

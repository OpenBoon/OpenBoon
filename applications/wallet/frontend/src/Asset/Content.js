import Link from 'next/link'
import { useRouter } from 'next/router'

import { constants, colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import SuspenseBoundary from '../SuspenseBoundary'
import Panel from '../Panel'
import Metadata from '../Metadata'
import AssetDelete from '../AssetDelete'

import InformationSvg from '../Icons/information.svg'
import BackSvg from '../Icons/back.svg'
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
        }}
      >
        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            width: '100%',
            overflowY: 'hidden',
          }}
        >
          <div
            css={{
              paddingLeft: spacing.base,
              paddingRight: spacing.base,
              backgroundColor: colors.structure.lead,
              color: colors.structure.steel,
              boxShadow: constants.boxShadows.navBar,
              marginBottom: spacing.hairline,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              width: '100%',
            }}
          >
            <Link
              href={`/[projectId]/visualizer${idString}${queryString}`}
              as={`/${projectId}/visualizer${idString}${queryString}`}
              passHref
            >
              <Button
                variant={VARIANTS.NEUTRAL}
                css={{
                  padding: spacing.base,
                  color: colors.structure.steel,
                  borderRadius: constants.borderRadius.small,
                  ':hover': {
                    color: colors.structure.white,
                  },
                }}
              >
                <BackSvg height={ICON_SIZE} />
              </Button>
            </Link>
          </div>
          <SuspenseBoundary>
            <AssetAsset projectId={projectId} assetId={assetId} />
          </SuspenseBoundary>
        </div>

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

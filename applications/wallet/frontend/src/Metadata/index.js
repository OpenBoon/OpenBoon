import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

import InformationSvg from '../Icons/information.svg'

import Resizeable from '../Resizeable'
import SuspenseBoundary from '../SuspenseBoundary'

import MetadataContent from './Content'
import MetadataSelect from './Select'

export const WIDTH = 400

const IMG_WIDTH = 20
const PADDING = spacing.normal

const Metadata = () => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  return (
    <Resizeable
      initialWidth={WIDTH}
      minWidth={IMG_WIDTH + PADDING * 2}
      storageName="metadata-width"
    >
      <div
        css={{
          backgroundColor: colors.structure.mattGrey,
          marginTop: spacing.hairline,
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: constants.boxShadows.metadata,
        }}
      >
        <div
          css={{
            display: 'flex',
            height: constants.navbar.height,
            alignItems: 'center',
            borderBottom: constants.borders.divider,
            padding: PADDING,
          }}
        >
          <InformationSvg
            width={IMG_WIDTH}
            color={assetId ? colors.structure.white : colors.structure.steel}
            css={{ minWidth: IMG_WIDTH }}
          />
          <div
            css={{
              padding: PADDING,
              color: assetId ? colors.structure.white : colors.structure.steel,
              fontWeight: typography.weight.bold,
              whiteSpace: 'nowrap',
            }}
          >
            ASSET METADATA
          </div>
        </div>

        {assetId ? (
          <SuspenseBoundary key={assetId}>
            <MetadataContent projectId={projectId} assetId={assetId} />
          </SuspenseBoundary>
        ) : (
          <MetadataSelect />
        )}
      </div>
    </Resizeable>
  )
}

export default Metadata

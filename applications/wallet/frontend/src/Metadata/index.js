import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

import InformationSvg from '../Icons/information.svg'

import Resizeable from '../Resizeable'
import SuspenseBoundary from '../SuspenseBoundary'

import MetadataContent from './Content'
import MetadataSelect from './Select'

export const WIDTH = 400

const Metadata = () => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  return (
    <Resizeable
      minWidth={WIDTH}
      storageName="metadata-width"
      position="left"
      onMouseUp={() => {}}
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
            padding: spacing.normal,
          }}
        >
          <InformationSvg
            width={20}
            color={assetId ? colors.structure.white : colors.structure.steel}
          />
          <div
            css={{
              padding: spacing.normal,
              color: assetId ? colors.structure.white : colors.structure.steel,
              fontWeight: typography.weight.bold,
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

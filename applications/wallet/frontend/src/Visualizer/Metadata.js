import { useRouter } from 'next/router'
import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import JsonDisplay from '../JsonDisplay'

import InformationSvg from './information.svg'

import assetShape from '../Asset/shape'

export const WIDTH = 400

const VisualizerMetadata = ({ assets }) => {
  const {
    query: { id },
  } = useRouter()

  const asset = !!id && assets.find(({ id: assetId }) => assetId === id)

  const { metadata: { source: { filename } = {} } = {} } = asset || {}

  return (
    <div
      css={{
        backgroundColor: colors.structure.mattGrey,
        marginTop: spacing.hairline,
        height: '100%',
        width: WIDTH,
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
          color={id ? colors.structure.white : colors.structure.steel}
        />
        <div
          css={{
            padding: spacing.normal,
            color: id ? colors.structure.white : colors.structure.steel,
            fontWeight: typography.weight.bold,
          }}
        >
          ASSET METADATA
        </div>
      </div>
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            color: filename ? colors.signal.sky.base : colors.key.one,
            fontStyle: filename ? '' : typography.style.italic,
          }}
        >
          {filename || 'Select an asset to view its metadata'}
        </div>
      </div>
      <div
        css={{
          height: '100%',
          overflow: 'auto',
          backgroundColor: id
            ? colors.structure.coal
            : colors.structure.mattGrey,
          padding: spacing.normal,
          paddingBottom: 0,
        }}
      >
        <JsonDisplay json={asset} />
      </div>
    </div>
  )
}

VisualizerMetadata.propTypes = {
  assets: PropTypes.arrayOf(assetShape).isRequired,
}

export default VisualizerMetadata

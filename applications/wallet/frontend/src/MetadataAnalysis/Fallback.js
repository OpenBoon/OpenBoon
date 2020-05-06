import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'
import { formatDisplayName } from '../Metadata/helpers'
import MetadataPrettyRow from '../Metadata/PrettyRow'

const MetadataAnalysisFallback = ({ name, value, path }) => {
  return (
    <>
      <div
        css={{
          borderTop: constants.borders.divider,
          ':hover': {
            div: {
              svg: {
                display: 'inline-block',
              },
            },
          },
        }}
      >
        <div
          css={{
            fontFamily: 'Roboto Condensed',
            color: colors.structure.steel,
            padding: spacing.normal,
            paddingBottom: 0,
            flex: 1,
          }}
        >
          <span title={`${path.toLowerCase()}.${name}`}>
            {formatDisplayName({ name })}
          </span>
        </div>
        <div />
      </div>

      <div
        css={{
          paddingLeft: spacing.normal,
          paddingRight: spacing.normal,
        }}
      >
        {Object.entries(value).map(([k, v]) => (
          <MetadataPrettyRow
            key={k}
            name={k}
            value={v}
            path={`${path.toLowerCase()}.${name}`}
          />
        ))}
      </div>
    </>
  )
}

MetadataAnalysisFallback.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    type: PropTypes.string.isRequired,
    predictions: PropTypes.arrayOf(PropTypes.shape({})),
  }).isRequired,
  path: PropTypes.string.isRequired,
}
export default MetadataAnalysisFallback

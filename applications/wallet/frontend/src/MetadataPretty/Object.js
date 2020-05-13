import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import { formatDisplayName } from '../Metadata/helpers'

import MetadataPrettyLabels from './Labels'
import MetadataPrettyContent from './Content'
import MetadataPrettySimilarity from './Similarity'
// eslint-disable-next-line import/no-cycle
import MetadataPrettyRow from './Row'

const MetadataPrettyObject = ({ name, value, path }) => {
  switch (value.type) {
    case 'labels':
      return <MetadataPrettyLabels name={name} value={value} />

    case 'content':
      return <MetadataPrettyContent name={name} value={value} />

    case 'similarity':
      return <MetadataPrettySimilarity name={name} value={value} />

    default:
      return (
        <>
          <div
            css={{
              borderTop: constants.borders.divider,
              ':hover': { div: { svg: { display: 'inline-block' } } },
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
            css={{ paddingLeft: spacing.normal, paddingRight: spacing.normal }}
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
}

MetadataPrettyObject.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.shape({}),
  ]).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettyObject

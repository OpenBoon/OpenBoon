import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { formatDisplayName } from '../Metadata/helpers'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataPrettyPredictions from './Predictions'
import MetadataPrettyContent from './Content'
import MetadataPrettySimilarity from './Similarity'
import MetadataPrettyRow from './Row'

const MetadataPrettySwitch = ({ name, value, path }) => {
  // Sort Analysis modules alphabetically
  const sortedValue =
    path === 'analysis'
      ? Object.keys(value)
          .sort()
          .reduce((obj, key) => ({ ...obj, [key]: value[key] }), {})
      : value

  if (Array.isArray(sortedValue)) {
    return sortedValue.map((attribute, index) => (
      <div
        // eslint-disable-next-line react/no-array-index-key
        key={`${path}${index}`}
        css={{
          width: '100%',
          '&:not(:first-of-type)': {
            borderTop: constants.borders.large.iron,
          },
        }}
      >
        {Object.entries(attribute).map(([k, v]) => (
          <MetadataPrettySwitch key={k} name={k} value={v} path={path} />
        ))}
      </div>
    ))
  }

  if (typeof sortedValue === 'object') {
    switch (sortedValue.type) {
      case 'labels':
        return (
          <MetadataPrettyPredictions
            name={name}
            value={sortedValue}
            path={path}
          />
        )

      case 'content':
        return <MetadataPrettyContent name={name} value={sortedValue} />

      case 'similarity':
        return (
          <div
            css={{
              '&:not(:first-of-type)': {
                borderTop: constants.borders.large.smoke,
              },
            }}
          >
            <SuspenseBoundary isTransparent>
              <MetadataPrettySimilarity
                name={name}
                value={sortedValue}
                path={path}
              />
            </SuspenseBoundary>
          </div>
        )

      default:
        return (
          <>
            {!!name && (
              <div css={{ borderTop: constants.borders.regular.smoke }}>
                <div
                  css={{
                    fontFamily: typography.family.condensed,
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
            )}

            <div css={name ? { paddingLeft: spacing.normal } : {}}>
              {Object.entries(sortedValue).map(([k, v]) => (
                <MetadataPrettySwitch
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

  return <MetadataPrettyRow name={name} value={sortedValue} path={path} />
}

MetadataPrettySwitch.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.shape({}),
    PropTypes.array,
  ]).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettySwitch

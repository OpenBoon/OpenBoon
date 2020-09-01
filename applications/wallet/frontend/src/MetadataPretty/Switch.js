import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { formatDisplayName } from '../Metadata/helpers'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataPrettyPredictions from './Predictions'
import MetadataPrettyContent from './Content'
import MetadataPrettySimilarity from './Similarity'
import MetadataPrettyRow from './Row'

const MetadataPrettySwitch = ({ name, value, path }) => {
  if (Array.isArray(value)) {
    return value.map((attribute, index) => (
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

  if (typeof value === 'object') {
    switch (value.type) {
      case 'single-label':
        return (
          <MetadataPrettyPredictions
            name={name}
            value={{ predictions: [value] }}
            path={path}
          />
        )

      case 'labels':
        return (
          <MetadataPrettyPredictions name={name} value={value} path={path} />
        )

      case 'content':
        return <MetadataPrettyContent name={name} value={value} />

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
              <MetadataPrettySimilarity name={name} value={value} path={path} />
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
              {Object.entries(value).map(([k, v]) => (
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

  return <MetadataPrettyRow name={name} value={value} path={path} />
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

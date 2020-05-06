import PropTypes from 'prop-types'

import { spacing, typography, constants } from '../Styles'

import FiltersMenuOption from './MenuOption'

const FiltersMenuSection = ({ path, attribute, value, filters, onClick }) => {
  const fullPath = `${path}.${attribute}`

  if (Array.isArray(value) && value.length === 0) return null

  if (Array.isArray(value)) {
    return (
      <FiltersMenuOption
        key={fullPath}
        option={fullPath}
        label={attribute}
        filters={filters}
        onClick={onClick({
          type: value[0],
          attribute: fullPath,
        })}
      />
    )
  }

  return (
    <div
      key={fullPath}
      css={{
        marginLeft: -spacing.normal,
        marginRight: -spacing.normal,
        padding: spacing.moderate,
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
        borderTop: constants.borders.largeDivider,
        ':first-of-type': {
          paddingTop: 0,
          borderTop: 'none',
        },
        ':last-of-type': {
          paddingBottom: 0,
        },
      }}
    >
      <h4
        css={{
          fontFamily: 'Roboto Mono',
          fontWeight: typography.weight.regular,
        }}
      >
        {attribute}
      </h4>

      {Object.entries(value).map(([subKey, subValue]) => (
        <FiltersMenuSection
          key={subKey}
          path={fullPath}
          attribute={subKey}
          value={subValue}
          filters={filters}
          onClick={onClick}
        />
      ))}
    </div>
  )
}

FiltersMenuSection.propTypes = {
  path: PropTypes.string.isRequired,
  attribute: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.array, PropTypes.object]).isRequired,
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      attribute: PropTypes.string,
      values: PropTypes.shape({}),
    }).isRequired,
  ).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default FiltersMenuSection

import PropTypes from 'prop-types'

import { spacing, typography, constants } from '../Styles'

import FiltersMenuOption from './MenuOption'

const FiltersMenuSection = ({ path, attribute, value, onClick }) => {
  if (Array.isArray(value) && value.length === 0) return null

  if (Array.isArray(value)) {
    return (
      <FiltersMenuOption
        key={attribute}
        option={attribute}
        onClick={onClick({
          type: value[0],
          attribute: `${path}.${attribute}`,
        })}
      />
    )
  }

  return (
    <div
      key={attribute}
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

      {Object.entries(value).map(([subSubKey, subSubValue]) => (
        <FiltersMenuOption
          key={subSubKey}
          option={subSubKey}
          onClick={onClick({
            type: subSubValue[0],
            attribute: `${path}.${attribute}.${subSubKey}`,
          })}
        />
      ))}
    </div>
  )
}

FiltersMenuSection.propTypes = {
  path: PropTypes.string.isRequired,
  attribute: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.array, PropTypes.object]).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default FiltersMenuSection

import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing, colors, constants } from '../Styles'

import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

const OFFSET = 32

const FiltersMenuOption = ({ option, label, filters, onClick }) => {
  const isEnabled = filters.find(({ attribute }) => attribute === option)

  return (
    <div
      key={option}
      css={{
        marginLeft: OFFSET,
        borderTop: constants.borders.regular.smoke,
        ':first-of-type': {
          borderTop: 'none',
        },
      }}
    >
      <div
        css={{
          color: colors.structure.zinc,
          padding: spacing.base,
          marginLeft: -OFFSET,
          ':hover': {
            backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
            color: colors.structure.white,
            svg: { color: colors.structure.white },
          },
        }}
      >
        <Checkbox
          variant={CHECKBOX_VARIANTS.SMALL}
          option={{
            value: option,
            label,
            initialValue: !!isEnabled,
            isDisabled: !!isEnabled,
          }}
          onClick={onClick}
        />
      </div>
    </div>
  )
}

FiltersMenuOption.propTypes = {
  option: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default FiltersMenuOption

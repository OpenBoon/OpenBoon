import {
  ListboxInput,
  ListboxButton,
  ListboxList,
  ListboxPopover,
} from '@reach/listbox'
import PropTypes from 'prop-types'

import listboxShape from './shape'

import ChevronSvg from '../Icons/chevron.svg'

import { constants, spacing, colors, typography } from '../Styles'

import ListboxOptions from './Options'

const ICON_SIZE = 20
const MAX_HEIGHT = 350

const Listbox = ({ label, options, onChange, value, placeholder }) => {
  return (
    <label>
      <div
        css={{
          paddingBottom: spacing.base,
          color: colors.structure.white,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.medium,
        }}
      >
        {label}
      </div>
      <ListboxInput
        defaultValue={value}
        css={{
          width: '100%',
          display: 'flex',
          outline: constants.borders.regular.transparent,
          border: 'none',
          backgroundColor: colors.structure.steel,
          borderRadius: constants.borderRadius.small,
          cursor: 'pointer',
          color: colors.structure.white,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.medium,
        }}
        onChange={(v) => onChange({ value: v })}
      >
        <ListboxButton
          css={{
            flex: 1,
            border: 'none',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: spacing.base,
          }}
          arrow={
            <ChevronSvg height={ICON_SIZE} color={colors.structure.white} />
          }
        >
          {placeholder}
        </ListboxButton>
        <ListboxPopover
          css={{
            ':focus-within': {
              outline: 'none',
              boxShadow: 'none',
            },
            backgroundColor: colors.structure.steel,
            padding: 0,
            outline: 'none',
            border: 'none',
          }}
        >
          <ListboxList
            css={{
              margin: 0,
              overflow: 'auto',
              height: 'auto',
              maxHeight: MAX_HEIGHT,
              backgroundColor: colors.structure.steel,
              color: colors.structure.white,
              borderRadius: constants.borderRadius.small,
              fontWeight: typography.weight.medium,
              paddingTop: spacing.base,
              paddingBottom: spacing.base,
            }}
          >
            <ListboxOptions options={options} />
          </ListboxList>
        </ListboxPopover>
      </ListboxInput>
    </label>
  )
}

Listbox.propTypes = {
  label: PropTypes.string.isRequired,
  options: listboxShape.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
  placeholder: PropTypes.string.isRequired,
}

export default Listbox

import { useRouter } from 'next/router'

import { colors, constants } from '../Styles'

import FilterSvg from '../Icons/filter.svg'

import { cleanup } from './helpers'

const INDICATOR_SIZE = 10
const BORDER = `2px solid ${colors.structure.lead}`

const FiltersIcon = () => {
  const {
    query: { query },
  } = useRouter()

  const q = cleanup({ query })

  const hasFilters = q && q !== 'W10='
  const hasDisabledFilters = query && !hasFilters

  return (
    <div css={{ position: 'relative' }}>
      {(hasFilters || hasDisabledFilters) && (
        <div
          css={{
            backgroundColor: hasDisabledFilters
              ? colors.structure.steel
              : colors.key.one,
            width: INDICATOR_SIZE,
            height: INDICATOR_SIZE,
            borderRadius: INDICATOR_SIZE,
            border: BORDER,
            position: 'absolute',
            top: -3,
            right: -2,
          }}
        />
      )}
      <FilterSvg height={constants.icons.regular} />
    </div>
  )
}

export default FiltersIcon

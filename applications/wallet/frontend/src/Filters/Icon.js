import { useRouter } from 'next/router'

import { colors } from '../Styles'

import FilterSvg from '../Icons/filter.svg'

import { cleanup } from './helpers'

const ICON_SIZE = 20
const INDICATOR_SIZE = 10
const BORDER = `2px solid ${colors.structure.lead}`

const FiltersIcon = () => {
  const {
    query: { query },
  } = useRouter()

  const q = cleanup({ query })

  const hasFilters = q && q !== 'W10='

  return (
    <div css={{ position: 'relative' }}>
      {hasFilters && (
        <div
          css={{
            backgroundColor: colors.key.one,
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
      <FilterSvg height={ICON_SIZE} />
    </div>
  )
}

export default FiltersIcon

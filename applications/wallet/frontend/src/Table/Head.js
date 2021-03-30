import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { colors, constants, spacing, typography } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import KebabSvg from '../Icons/kebab.svg'
import SortSvg from '../Icons/sort.svg'

import { getQueryString } from '../Fetch/helpers'

const TableHead = ({ column, expandColumn }) => {
  const label = column?.label || column
  const key = column?.key || ''

  const {
    pathname,
    query,
    query: { sort = '' },
  } = useRouter()

  const sortKey = sort.replace(/^-/, '')
  const isAscSort = sort === sortKey
  const newSortDirection = key === sortKey && isAscSort ? '-' : ''
  const newSort = `${newSortDirection}${key}`

  const queryParam = getQueryString({
    query: query.query,
    sort: newSort,
    filter: query.filter,
  })
  const href = `${pathname}${queryParam}`
  const as = href
    .split('/')
    .map((s) => s.replace(/\[(.*)\]/gi, (_, group) => query[group]))
    .join('/')

  return (
    <th
      css={{
        textAlign: 'left',
        whiteSpace: 'pre-line',
        verticalAlign: 'bottom',
        fontSize: typography.size.regular,
        lineHeight: typography.height.regular,
        fontWeight: typography.weight.medium,
        color: colors.structure.pebble,
        backgroundColor: colors.structure.iron,
        padding: 0,
        borderBottom: constants.borders.regular.mattGrey,
        // hack to resize the Actions column to its smallest possible width
        width: label === '#Actions#' ? 1 : 'auto',
        [`:nth-of-type(${expandColumn})`]: { width: '100%' },
        '&:not(:last-child)': {
          borderRight: constants.borders.regular.mattGrey,
        },
      }}
    >
      {label === '#Actions#' && (
        <div
          css={{
            display: 'flex',
            padding: `${spacing.moderate}px ${spacing.normal}px`,
          }}
        >
          <KebabSvg height={constants.icons.regular} />
        </div>
      )}

      {label === '#Checkmark#' && (
        <div
          css={{
            display: 'flex',
            padding: `${spacing.moderate}px ${spacing.normal}px`,
          }}
        >
          <CheckmarkSvg height={constants.icons.regular} />
        </div>
      )}

      {label !== '#Actions#' && label !== '#Checkmark#' && column?.label && (
        <Link href={href} as={as}>
          <a
            css={{
              display: 'flex',
              backgroundColor: colors.structure.transparent,
              border: 'none',
              fontSize: 'inherit',
              fontWeight: 'inherit',
              color: 'inherit',
              cursor: 'pointer',
              width: '100%',
              padding: `${spacing.moderate}px ${spacing.normal}px`,
              ':hover': {
                textDecoration: 'none',
                userSelect: 'none',
                svg: {
                  color: colors.structure.white,
                  transform:
                    key === sortKey && isAscSort
                      ? 'rotate(0deg)'
                      : 'rotate(-180deg)',
                },
              },
            }}
          >
            {label}
            <SortSvg
              height={constants.icons.small}
              css={{
                marginLeft: spacing.small,
                color:
                  key === sortKey
                    ? colors.key.two
                    : colors.structure.transparent,
                transform:
                  key === sortKey && isAscSort ? 'rotate(-180deg)' : '',
              }}
            />
          </a>
        </Link>
      )}

      {label !== '#Actions#' && label !== '#Checkmark#' && !column?.label && (
        <div css={{ padding: `${spacing.moderate}px ${spacing.normal}px` }}>
          {label}
        </div>
      )}
    </th>
  )
}

TableHead.propTypes = {
  column: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.shape({ key: PropTypes.string, label: PropTypes.string }),
  ]).isRequired,
  expandColumn: PropTypes.number.isRequired,
}

export default TableHead

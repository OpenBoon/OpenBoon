import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import Pagination from '../Pagination'

import GearSvg from '../Icons/gear.svg'
import TableException from './Exception'
import TableRefresh from './Refresh'

const SIZE = 20

const TableContent = ({
  url,
  columns,
  expandColumn,
  renderEmpty,
  renderRow,
  legend,
}) => {
  const {
    query: { page = 1 },
  } = useRouter()

  const parsedPage = parseInt(page, 10)
  const from = parsedPage * SIZE - SIZE

  const {
    data: { count = 0, results },
    revalidate,
  } = useSWR(`${url}?from=${from}&size=${SIZE}`)

  return (
    <div css={{ height: count === 0 ? '100%' : 'auto' }}>
      <div
        css={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          paddingBottom: spacing.normal,
        }}
      >
        <h3
          css={{
            color: colors.structure.zinc,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            fontWeight: typography.weight.regular,
          }}
        >
          Number of {legend}: {count}
        </h3>
        <TableRefresh onClick={revalidate} legend={legend} />
      </div>
      <table
        css={{
          width: '100%',
          height: count === 0 ? '100%' : 'auto',
          borderSpacing: 0,
          boxShadow: constants.boxShadows.default,
          whiteSpace: 'nowrap',
          tr: {
            backgroundColor: colors.structure.lead,
            '&:nth-of-type(2n)': {
              backgroundColor: colors.structure.mattGrey,
            },
            ':hover': {
              backgroundColor: colors.structure.iron,
              boxShadow: constants.boxShadows.tableRow,
              '.gear': {
                color: colors.structure.zinc,
              },
              td: {
                border: constants.borders.tableRow,
                borderLeft: '0',
                borderRight: '0',
                '&:first-of-type': {
                  borderLeft: constants.borders.tableRow,
                },
                '&:last-of-type': {
                  borderRight: constants.borders.tableRow,
                },
              },
            },
          },
          td: {
            maxWidth: `calc(100vw / ${columns.length - 1})`,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            fontWeight: typography.weight.extraLight,
            color: colors.structure.pebble,
            padding: `${spacing.base}px ${spacing.normal}px`,
            border: constants.borders.transparent,
            borderLeft: '0',
            borderRight: '0',
            ':first-of-type': {
              borderLeft: constants.borders.transparent,
            },
            ':last-of-type': {
              borderRight: constants.borders.transparent,
              overflow: 'visible',
            },
          },
        }}
      >
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column}
                css={{
                  textAlign: 'left',
                  fontSize: typography.size.regular,
                  lineHeight: typography.height.regular,
                  fontWeight: typography.weight.medium,
                  color: colors.structure.pebble,
                  backgroundColor: colors.structure.iron,
                  padding: `${spacing.moderate}px ${spacing.normal}px`,
                  borderBottom: constants.borders.default,
                  [`:nth-of-type(${expandColumn})`]: { width: '100%' },
                  '&:not(:last-child)': {
                    borderRight: constants.borders.default,
                  },
                }}
              >
                {column === '#Actions#' ? (
                  <div css={{ display: 'flex' }}>
                    <GearSvg width={20} />
                  </div>
                ) : (
                  column
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {count === 0 ? (
            <TableException numColumns={columns.length}>
              {renderEmpty}
            </TableException>
          ) : (
            results.map((result) => renderRow({ result, revalidate }))
          )}
        </tbody>
      </table>

      <div>&nbsp;</div>

      {count > 0 && (
        <Pagination
          currentPage={parsedPage}
          totalPages={Math.ceil(count / SIZE)}
        />
      )}

      <div>&nbsp;</div>
    </div>
  )
}

TableContent.propTypes = {
  url: PropTypes.string.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string).isRequired,
  expandColumn: PropTypes.number.isRequired,
  renderEmpty: PropTypes.node.isRequired,
  renderRow: PropTypes.func.isRequired,
  legend: PropTypes.string.isRequired,
}

export default TableContent

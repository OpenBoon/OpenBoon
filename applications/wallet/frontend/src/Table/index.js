import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import TableContent from './Content'

import { colors, constants, spacing, typography } from '../Styles'

import Pagination from '../Pagination'

const SIZE = 20

const Table = ({ url, columns, renderEmpty, renderRow }) => {
  const {
    query: { page = 1 },
  } = useRouter()

  const parsedPage = parseInt(page, 10)
  const from = parsedPage * SIZE - SIZE

  const { data: { count = 0, results } = {}, error, revalidate } = useSWR(
    `${url}?from=${from}&size=${SIZE}`,
  )

  return (
    <div>
      <table
        css={{
          width: '100%',
          borderSpacing: 0,
          boxShadow: constants.boxShadows.table,
          whiteSpace: 'nowrap',
          tr: {
            backgroundColor: colors.structure.lead,
            '.gear': {
              opacity: 0,
              '&:focus': {
                opacity: 1,
              },
            },
            '&:nth-of-type(2n)': {
              backgroundColor: colors.structure.mattGrey,
            },
            ':hover': {
              backgroundColor: colors.structure.iron,
              boxShadow: constants.boxShadows.tableRow,
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
              '.gear': {
                opacity: 1,
              },
            },
          },
          td: {
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
            },
          },
        }}>
        <thead>
          <tr>
            {columns.map(column => (
              <th
                key={column}
                css={{
                  textAlign: 'left',
                  fontSize: typography.size.kilo,
                  lineHeight: typography.height.kilo,
                  fontWeight: typography.weight.medium,
                  color: colors.structure.pebble,
                  backgroundColor: colors.structure.iron,
                  padding: `${spacing.moderate}px ${spacing.normal}px`,
                  borderBottom: constants.borders.default,
                  ':nth-of-type(2)': { width: '100%' },
                  '&:not(:last-child)': {
                    borderRight: constants.borders.default,
                  },
                }}>
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          <TableContent
            numColumns={columns.length}
            hasError={!!error}
            isLoading={!results}
            results={results || []}
            renderEmpty={renderEmpty}
            renderRow={renderRow}
            revalidate={revalidate}
          />
        </tbody>
      </table>

      <div>&nbsp;</div>

      {count > 0 && !error && (
        <Pagination
          currentPage={parsedPage}
          totalPages={Math.ceil(count / SIZE)}
        />
      )}
    </div>
  )
}

Table.propTypes = {
  url: PropTypes.string.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string).isRequired,
  renderEmpty: PropTypes.node.isRequired,
  renderRow: PropTypes.func.isRequired,
}

export default Table

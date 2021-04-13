import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import { getQueryString } from '../Fetch/helpers'

import FetchAhead from '../Fetch/Ahead'
import Pagination from '../Pagination'

import TableException from './Exception'
import TableRefresh from './Refresh'
import TableHead from './Head'

const SIZE = 20

const TableContent = ({
  url,
  columns,
  expandColumn,
  renderEmpty,
  renderRow,
  legend,
  refreshKeys,
  refreshButton,
}) => {
  const {
    query: { page = 1, sort, search = '' },
  } = useRouter()

  const parsedPage = parseInt(page, 10)
  const from = parsedPage * SIZE - SIZE
  const queryParam =
    page > 1
      ? getQueryString({ from, size: SIZE, ordering: sort, search })
      : getQueryString({ ordering: sort, search })

  const {
    data: { count = 0, results, previous, next },
    mutate: revalidate,
  } = useSWR(`${url}${queryParam}`)

  return (
    <div css={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div
        css={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          paddingBottom: spacing.normal,
          flexShrink: 0,
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
          {legend}: {count}
        </h3>

        {refreshButton && (
          <TableRefresh
            onClick={revalidate}
            refreshKeys={refreshKeys}
            legend={legend}
          />
        )}
      </div>

      <div css={{ flex: 1, position: 'relative' }}>
        <div
          css={
            count === 0
              ? { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }
              : {}
          }
        >
          <table
            css={{
              height: count === 0 ? '100%' : 'auto',
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
                  '.actions': {
                    color: colors.structure.zinc,
                  },
                  td: {
                    border: constants.borders.regular.steel,
                    borderLeft: '0',
                    borderRight: '0',
                    '&:first-of-type': {
                      borderLeft: constants.borders.regular.steel,
                    },
                    '&:last-of-type': {
                      borderRight: constants.borders.regular.steel,
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
                border: constants.borders.regular.transparent,
                borderLeft: '0',
                borderRight: '0',
                ':first-of-type': {
                  borderLeft: constants.borders.regular.transparent,
                },
                ':last-of-type': {
                  borderRight: constants.borders.regular.transparent,
                  overflow: 'visible',
                },
              },
            }}
          >
            <thead>
              <tr>
                {columns.map((column) => {
                  const label = column?.label || column
                  return (
                    <TableHead
                      key={label}
                      column={column}
                      expandColumn={expandColumn}
                    />
                  )
                })}
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
        </div>

        {count > 0 && <div>&nbsp;</div>}

        {count > 0 && (
          <Pagination
            currentPage={parsedPage}
            totalPages={Math.ceil(count / Math.max(SIZE, results.length))}
          />
        )}

        {previous && <FetchAhead url={previous} />}

        {next && <FetchAhead url={next} />}

        {count > 0 && <div>&nbsp;</div>}
      </div>
    </div>
  )
}

TableContent.propTypes = {
  url: PropTypes.string.isRequired,
  columns: PropTypes.arrayOf(
    PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.shape({ key: PropTypes.string, label: PropTypes.string }),
    ]).isRequired,
  ).isRequired,
  expandColumn: PropTypes.number.isRequired,
  renderEmpty: PropTypes.node.isRequired,
  renderRow: PropTypes.func.isRequired,
  legend: PropTypes.string.isRequired,
  refreshKeys: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  refreshButton: PropTypes.bool.isRequired,
}

export default TableContent

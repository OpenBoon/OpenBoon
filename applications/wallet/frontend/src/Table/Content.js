import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

import FetchAhead from '../Fetch/Ahead'
import Pagination from '../Pagination'

import KebabSvg from '../Icons/kebab.svg'

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
  refreshKeys,
  refreshButton,
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
          Number of {legend}: {count}
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
                      borderBottom: constants.borders.regular.mattGrey,
                      // hack to resize the Actions column to its smallest possible width
                      width: column === '#Actions#' ? 1 : 'auto',
                      [`:nth-of-type(${expandColumn})`]: { width: '100%' },
                      '&:not(:last-child)': {
                        borderRight: constants.borders.regular.mattGrey,
                      },
                    }}
                  >
                    {column === '#Actions#' && (
                      <div css={{ display: 'flex' }}>
                        <KebabSvg height={constants.icons.regular} />
                      </div>
                    )}

                    {column === '#Checkmark#' && (
                      <div css={{ display: 'flex' }}>
                        <CheckmarkSvg height={constants.icons.regular} />
                      </div>
                    )}

                    {column !== '#Actions#' &&
                      column !== '#Checkmark#' &&
                      column}
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
        </div>

        {count > 0 && <div>&nbsp;</div>}

        {count > 0 && (
          <Pagination
            currentPage={parsedPage}
            totalPages={Math.ceil(count / SIZE)}
          />
        )}

        {count > 0 && parsedPage < Math.ceil(count / SIZE) && (
          <FetchAhead url={`${url}?from=${from + SIZE}&size=${SIZE}`} />
        )}

        {count > 0 && parsedPage > 1 && (
          <FetchAhead url={`${url}?from=${from - SIZE}&size=${SIZE}`} />
        )}

        {count > 0 && <div>&nbsp;</div>}
      </div>
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
  refreshKeys: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  refreshButton: PropTypes.bool.isRequired,
}

export default TableContent

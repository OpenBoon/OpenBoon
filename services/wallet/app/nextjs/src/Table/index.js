import PropTypes from 'prop-types'
import { useTable } from 'react-table'
import { css } from '@emotion/core'
import { colors, spacing } from '../Styles'

const Table = ({ columns, data }) => {
  const {
    getTableProps,
    getTableBodyProps,
    headerGroups,
    rows,
    prepareRow,
  } = useTable({ columns, data })

  const thCSS = css`
     {
      height: ${spacing.spacious}px;
      font-weight: 200;
      color: ${colors.grey2};
      background-color: ${colors.grey1};
      padding: ${spacing.moderate}px ${spacing.normal}px;
      border-bottom: 1px solid ${colors.grey5};
      :not(:last-child) {
        border-right: 1px solid ${colors.grey5};
      }
    }
  `

  const tdCSS = css`
     {
      height: ${spacing.spacious}px;
      font-weight: 200;
      color: ${colors.grey2};
      padding: ${spacing.base}px ${spacing.normal}px;
    }
  `

  const trCSS = css`
     {
      background-color: ${colors.grey4};
      :nth-of-type(2n) {
        background-color: ${colors.grey3};
      }
    }
  `

  return (
    <table {...getTableProps()} css={{ borderSpacing: 0 }}>
      <thead>
        {headerGroups.map(headerGroup => {
          const { key, ...rest } = headerGroup.getHeaderGroupProps()
          return (
            <tr key={key} {...rest}>
              {headerGroup.headers.map(column => {
                const { key, ...rest } = column.getHeaderProps()
                return (
                  <th key={key} {...rest} css={thCSS}>
                    <div className="Header__title" css={{ display: 'flex' }}>
                      {column.render('Header')}
                    </div>
                  </th>
                )
              })}
            </tr>
          )
        })}
      </thead>
      <tbody {...getTableBodyProps()}>
        {rows.map(row => {
          prepareRow(row)
          const { key, ...rest } = row.getRowProps()
          return (
            <tr key={key} {...rest} css={trCSS}>
              {row.cells.map(cell => {
                const { key, ...rest } = cell.getCellProps()
                return (
                  <td key={key} {...rest} css={tdCSS}>
                    {cell.render('Cell')}
                  </td>
                )
              })}
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

Table.propTypes = {
  columns: PropTypes.array.isRequired,
  data: PropTypes.array.isRequired,
}

export default Table

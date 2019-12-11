import { closeSidebar } from '../helpers'

describe('<Sidebar /> helpers', () => {
  describe('closeSidebar()', () => {
    it('should render properly closed', () => {
      const mockOnFunction = jest.fn((_, cb) => cb())
      const mockSetSidebarOpen = jest.fn()
      const mockOffFunction = jest.fn()

      require('next/router').__setMockOnFunction(mockOnFunction)
      require('next/router').__setMockOffFunction(mockOffFunction)

      const unmountCallback = closeSidebar({
        setSidebarOpen: mockSetSidebarOpen,
      })()

      expect(mockOnFunction).toHaveBeenCalled()
      expect(mockSetSidebarOpen).toHaveBeenCalledWith(false)

      unmountCallback()

      expect(mockOffFunction).toHaveBeenCalled()
    })
  })
})

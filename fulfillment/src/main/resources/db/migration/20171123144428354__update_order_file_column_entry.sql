UPDATE fulfillment.order_file_columns
SET
    columnlabel = 'Ordered quantity',
    datafieldlabel = 'fulfillment.header.ordered.quantity',
    keypath = 'orderedQuantity'
WHERE
    id = 'cd57f329-f549-4717-882e-ecbf98122c39';